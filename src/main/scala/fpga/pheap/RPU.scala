package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._

/*
* 操作分配问题：1个pheap clock = 3个clock，三个周期执行一个操作，如何分配？
* T[i]用来指示level i需要执行的操作；不同于脉动阵列，op[i]是对block[i+1]进行操作
* 所以我认为，
* 第一个周期：0->1的上升沿写入T[i]，T[i].position经过组合逻辑得到读地址并输入SRAM
* 第二个周期：1->2读出SRAM数据，进行比较....
* 第三个周期：感觉write信号在本周期准备好就行了，然后3->4上升沿写入；
* Q：Sram写入比寄存器慢，指的是Sram输入延迟长吗（说的是RPU产生写信号到上升沿来临前那段时间）
* */

/*
* 读写想法：
* 每个操作，涉及一个tuple的数据，包括一个父节点和两个兄弟子节点
* 读需要连续读两个兄弟，所以要一次性读两个连续节点
* 父节点和子节点不在同一片SRAM中，理论上可以同时读，
* 但是由于上层操作的子节点之一必然是下层操作的父节点，所以直接由上层传过来比较方便，即只读子节点
* 因为操作只改变父节点，所以就只写父节点（读写单位为两个节点，所以也要顺便把叔也写进去）
* */


// a RPU in the PHeap, one RPU per level
class RPU(val level : Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new Token) // from the previous RPU
        val token_out = Output(new Token) // to the next RPU

        val state = Input(new State) // read execute write

        val prev_dual_entry_in = Input()    // 上层操作的子节点传过来变为本层的父节点
        val next_dual_entry_out = Output()  // 本层子节点->下层

        // Sram是要在PHeap里定义吗，然后RPU把地址和使能传出去，PHeap根据信号调用read函数？

        // 读下层，一次性读两个子节点，第level+1层有2^level个节点，所以地址线需要level-1根
        val read = Output(Bool())
        val read_addr = Output(UInt( Math.max(1, level - 1).W ))
        val read_dual_entry = Input(Vec(2, Node()))

        // 写本层，一次性两个节点，所以地址线需要level-2根
        val write = Output(Bool())
        val write_addr = Output(UInt( Math.max(1, level - 1).W ))
        val write_dual_entry = Output(Bool())

        val is_left_in = Input(Bool())
        val is_left_out = Input(Bool())
    })

    // the operation to be performed in this level
    val token = RegInit(Token.default)
    val op = token.op // 这是reg类型
    token_out := token

    // position是以单个node为单位的，而读写以两个node为单位
    // 所以父节点的position刚好为兄弟子节点的address
    val next_addr = token.position

    val dual_entry = RegInit(Vec(2, Node.default))
    // TODO 待改进，因为这里node是wire类型
    val node = Mux(io.is_left_in, dual_entry(0), dual_entry(1))

    // 兄弟子节点
    val next_dual_entry = RegInit(Vec(2, Node.default))
    val left_entry = next_dual_entry(0)
    val right_entry = next_dual_entry(1)
    next_dual_entry_out := next_dual_entry

    val push_cmp = op.push < node.entry
    //val pop_cmp = left_entry < right_entry

    // TODO 状态转换这些待补充
    switch(state) {
        // 从上个block获得本tuple的父节点，并读子节点
        is(State.read) {
            dual_entry := io.prev_dual_entry_in     // reg TODO 这里的赋值应该和state变为read同时进行
            read := true.B                          // wire
            read_addr := next_addr                  // wire
            next_dual_entry := read_dual_entry      // reg 兄弟子节点下个上升沿读入寄存器
        }
        is(State.cmp) {
            /*
            * 比较问题：
            * replace需要三者选最小，
            *   1. 要么1个比较器比较两次，有两个比较延迟；
            *   2. 要么3个比较器同时比较，然后根据比较结果再做一点运算得出最小值；
            * 不太懂比较的时间与Sram读写的时间，哪个长哪个短
            * 感觉3logn的复杂度也不高
            * */


            // 先实现push：则只有push或nop
            // push：把值读出来并比较写入小值，大值传到下一级
            // TODO pheap中的value代表优先级，所以是大顶堆，本项目应该都是代表rank，所以是小顶堆？
            // push_cmp为true时即为push，此时还需检查一下目标节点是否存在，以计算capacity
            // 交换，或保持; done = op.push.existing == false
            when(push_cmp) {
                node.entry := op.push
                op.push := node.entry
                when(!node.existing) {
                    // TODO capacity可以像BBQ介绍的那样优化
                    node.capacity := node.capacity - 1
                }
            }
        }
        is(State.write) {
            // 准备好写数据
            // 写父节点
            write := true.B
            write_addr := token.position
            write_dual_entry := dual_entry

            // 子节点传给下个RPU接着用
            next_dual_entry_out := next_dual_entry

            // token传递 token_out := token

        }
    }



    // connect RPUs, this ~> next
    def ~>(next: RPU) = {
        // TODO
    }
}