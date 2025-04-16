package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node._

""""
    每三个时钟周期作为一个整体的clock
    每层都用一个RPU处理(不考虑RPU数量上的优化) 
    第一个clock:token读入新的token,计算position,left-child,right-child等数据
    第二个clock:读数据,包括本层的SRAM和下一层的SRAM,比较器处理
    第三个clock:写数据,准备向下一层传token等数据,可能发生写
    测试时每六个CPU clock传入一组新操作
""""

// 分层实现PHeap
// Q : 实现上的问题,每层RPU能否包含两层Sram?
// 这样能简化实现但是好像和原文不是完全相符
class RPU (val level : Int,val mem_type : String) extends Module {
    val io = IO(new Bundle {
        // token array 传递的数据
        val token_in = Input(new Token(level)) // 上一层RPU传入的token
        val token_out = Output(new Token(level + 1)) // 当前RPU向下一层传递的token

        // binary array 传递的数据
        val prev_mem_in = Input(new Node(level)) // 上一层传入的node,用于edq
        val mem_out = Output(new Node(level + 1)) // 用于输出当前的node
        val dual_mem_in = Input(Vec(2,new Node(level + 1))) // 下一层传入的node，包含两个子节点
        val dual_mem_out = Output(Vec(2,new Node(level))) // 下一层向上一层传的两个node
        val addr_out = Output(UInt) // 输出地址信号
        val addr_in = Input(UInt) // 输入的地址信号
    })

    // 用寄存器保存token
    val token = RegInit(Token.default(level))
    io.token_out := token

    // 指定memory的实现方式
    val mem = Module(new MemBlock(level,mem_type))
    io.mem_out := mem

    // 从token获取当前RPU操作状态
    switch (token.op) {
        is (token.op.push.existing && token.op.pop) {
            val state = State.edq
        }
        is (token.op.push.existing) {
            val state = State.enq
        }
        is (token.op.pop) {
            val state = State.deq
        }
        otherwise {
            val state = State.nop
        }
    }

    // 一些准备信号(组合逻辑)
    val i = token.position // 获取下标索引i
    val cur_l_index = PheapFunc.cal_local_index(i, level) // 当前要操作的结点的块内索引
    val lc_g_index = PheapFunc.get_lc_g_index(i) // 获取当前结点索引i的左孩子结点的索引(global)

    io.addr_out := lc_g_index // 向下一层传左孩子的global_index

    // 读左右孩子
    when (io.addr_in =/= 0 && state =/= State.nop) {
        val lc_l_index = PheapFunc.cal_local_index(lc_g_index, level) // 获取lc local_index
        // Q：这样读vec是否可行？或许应该修改read一次读出连续的两个node（希望在一个时钟周期内读出两个node）
        io.dual_mem_out(0) := read(lc_l_index, level)
        io.dual_mem_out(1) := read(lc_l_index + 1, level)
    }

    // FSM
    val idle :: cycle1 :: cycle2 :: cycle3 :: Nil = Enum(4) // 周期，还没想好用在哪

    switch(state) {
        is (State.enq) {
            val v = token.op.push
            val target_node = mem.read(cur_l_index, level)
            when (!target_node.value.existing) { // not-active
                mem.write(cur_l_index, v)
                // done
                io.token_out.op := Operator.nop
                io.token_out.position := 0.U
            }
            .otherwise { // active
                val swap = target_node.value < token.op.push
                when (swap) {
                    mem.write(cur_l_index, v) //swap
                    io.token_out.op.push := target_node
                    io.token_out.op.pop := token.op.pop
                    io.token_out.position := Mux(io.dual_mem_in(0).capacity > 0, lc_g_index, lc_g_index + 1.U)
                }
                .otherwise {
                    io.token_out := token
                }
            }
        }
        is (State.deq) {
            val is_lc = io.dual_mem_in(1) < io.dual_mem_in(0)
            mem.write(cur_l_index, Mux(is_lc, io.dual_mem_in(0),io.dual_mem_in(1)))
            // done
            io.token_out.op := DontCare
            io.token_out.position := Mux(is_lc, lc_g_index, lc_g_index + 1.U)
        }
        is (State.edq) {
            
        }
        otherwise {
            // do nothing
        }
    }

    // todo:connect all the rpus
    def ~> (next : RPU) {
        this.io.token_out <> next.io.token_in
        this.io.addr_out <> next.io.addr_in
        this.io.dual_mem_in <> next.io.dual_mem_out
    }

}