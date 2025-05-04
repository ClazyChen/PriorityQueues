package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node._
import fpga.pheap.Func._


// 分层实现PHeap,基于RPU(rank processing unit)
class RPU (val level : Int,val mem_type : String) extends Module {
    val io = IO(new Bundle {
        // token array 
        val token_in = Input(new Token(level)) 
        val token_out = Output(new Token(level + 1)) 
        // binary array 
        val pair_in = Input(new Pair(level + 1)) // 两个孩子节点
        val pair_out = Output(new Pair(level))
        val node_in = Input(new Node(level - 1)) // 从上层接收的node
        val node_out = Output(new Node(level)) 
        val position_in = Input(UInt(position_width(level).W)) // 上层传入的lc_position
        val position_out = Output(UInt(position_width(level + 1).W)) // 向下层传递的lc_position
        val read_children_in = Input(Bool()) // 读下层RPU
        val read_children_out = Output(Bool())
        val write_in = Input(Bool()) // 下层RPU写
        val write_out = Output(Bool())
    })
    
    // RPU内部存储结构
    val token = RegInit(Token.default(level))
    val mem = Module(new MemBlock(level, mem_type))
    io.pair_out := mem.io.pair_out
    mem.io.node_in := io.node_in
    io.node_out := mem.io.node_out
    mem.io.position_in := io.position_in
    mem.io.read_children := io.read_children
    mem.io.write := io.write

    // 信号初始化
    io.token_out := DontCare
    io.pair_out := DontCare
    io.node_out := DontCare
    io.position_out := 0.U
    io.read_children_out := false.B
    io.write_out := false.B

     // 设置比较器
    
    

    // 状态寄存器
    val rCycle0 :: rCycle1 :: rCycle2 :: Nil = Enum(3)
    val state = RegInit(rCycle0)

    // FSM
    switch (state) {
        is (rCycle0) {
            when (io.token_in.position) {
                token := io.token_in // 读入token
                io.read_child_enable := true.B // 读下一层的两个结点
                io.addr_out := lc_position // 左孩子全局索引     
                // 进入下一周期
                state := cycle2   
            }
            .otherwise {}
        }
        is (cycle2) {
            cur_node = mem.read(cal_local_index(i, level), level) // 读当前节点
            when (io.output_prev_enable) {
                io.lc_node_out := mem.read(read_left_index, level) // 读左孩子
                io.rc_node_out := mem.read(read_right_index, level) // 读右孩子
            }
            .otherwise {}
            // 进入下一周期
            state := cycle3 
        }
        is (cycle3) {
            // 状态转移 实现enqueue dequeue edq
            when (token.op.push.existing && !token.op.pop) {
                when (!cur_node.value.existing) {
                    val new_node = new Node(level) // 要写入sram的结点
                    new_node.value = token.op.push
                    new_node.capacity = cur_node.capacity - 1.U
                    mem.write(cal_local_index(i, level), new_node) // 写入
                }
                .otherwise {
                    val new_node = new Node(level)
                    when (swap_enable) {
                        new_node.value = token.op.push
                        new_node.capacity = cur_node.capacity
                        mem.write(cal_local_index(i, level),new_node) // swap
                        // 传递给下一层RPU
                        io.token_out.op.push.metadata := cur_node.metadata
                        io.token_out.op.push.rank := cur_node.rank
                    }.otherwise { // 不用swap
                        io.token_out.op.push.metadata := token.op.push.metadata
                        io.token_out.op.push.rank := token.op.push.rank
                    }
                    io.token_out.op.push.existing := token.op.push.existing
                    io.token_out.op.pop := token.op.pop
                    io.token_out.position := Mux(io.lc_node_in.capacity > 1, lc_position, rc_position)
                }
            }.elsewhen (token.op.pop && !token.op.push.existing) {
                val new_node = new Node(level) // 向上一层传递的node
                when (cmp_lc_rc) { // 左孩子的优先级高
                    new_node = lc_node
                    new_node.capacity = lc_node.capacity - 1.U
                }.otherwise { // 右孩子的优先级高
                    new_node = rc_node
                    new_node.capacity = rc_node.capacity - 1.U
                }
                mem.write(cal_local_index(i, level), new_node)
                io.token_out.op := token.op
                io.token_out.position := Mux(cmp_lc_rc, lc_position, rc_position)
            }.elsewhen (token.op.pop && token.op.push.existing) { // edq
                val new_node = new Node(level)

            }.otherwise {
                // do nothing
            }          
            state := cycle1
        }
        is (idle) {
            // idle状态，不需要额外操作
            when (io.token_in.push.existing || io.token_in.pop) {
                state := cycle1
            }
            io.token_out = Token.default(level)
        }
        otherwise {
            // do nothing
        }
    }

    // connect all rpus
    def ~> (next : RPU) {
        this.io.token_out <> next.io.token_in
        this.io.pair_in <> next.io.pair_out
        this.io.node_out <> next.io.node_in
        this.io.position_out <> next.io.position_in
        this.io.read_children_out <> next.io.read_children_in
        this.io.write_out <> next.io.write_in
    }

}