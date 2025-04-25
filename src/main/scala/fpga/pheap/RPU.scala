package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node._


// 分层实现PHeap
class RPU (val level : Int,val mem_type : String) extends Module {
    val io = IO(new Bundle {
        // token array 传递的数据
        val token_in = Input(new Token(level)) // 上一层RPU传入的token
        val token_out = Output(new Token(level + 1)) // 当前RPU向下一层传递的token
        // binary array 传递的数据
        val mem_out = Output(new Node(level + 1)) // 用于输出当前的node,用于外部接收,测试
        val lc_node_in = Input(new Node(level + 1)) // 左孩子输入
        val rc_node_in = Input(new Node(level + 1)) // 右孩子输入
        val lc_node_out = Output(new Node(level)) // 当前RPU作为左孩子传递给上层RPU的node
        val rc_node_out = Output(new Node(level)) // 当前RPU作为右孩子传递给上层RPU的node
        val addr_in = Input(UInt(position_width(level - 1).W)) // 上一层传入的position
        val addr_out = Output(UInt(position_width(level).W)) // 向下一层传入的position
        val read_child_enable = Output(Bool()) // 读子孩子结点的使能信号
        val output_prev_enable = Input(Bool()) // 向上一层传递两个结点的使能信号
    })

    // 用寄存器保存token
    val token = RegInit(Token.default(level))
    token := io.token_in
    // 指定memory的实现方式
    val mem = Module(new MemBlock(level,mem_type))

    // 组合逻辑辅助信号
    val read_left_index = cal_local_index(io.addr_in, level)
    val read_right_index = read_left_index + 1.U
    val lc_position =  get_lc_g_index(i) // 当前下标i的左孩子position
    val rc_position =  get_rc_g_index(i) // 当前下标i的右孩子position

    // 相关变量
    val i = io.token_in.position // 读出i
    val v = io.token_in.op.push // 读出v
    val cur_node = new Node(level) // 当前结点

     // 设置比较器
    val swap_enable = token.op.push < cur_node.value
    val cmp_lc_rc = io.lc_node_in.value < io.rc_node_in.value

    // 时钟周期 状态寄存器
    val idle :: cycle1 :: cycle2 :: cycle3 :: Nil = Enum(4)
    val state = RegInit(idle)

    // FSM
    switch (state) {
        // read token_in
        is (cycle1) {
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

    // connect all the rpus
    def ~> (next : RPU) {
        this.io.token_out <> next.io.token_in
        this.io.addr_out <> next.io.addr_in
        this.io.lc_node_in <> next.io.lc_node_out
        this.io.rc_node_in <> next.io.rc_node_out
        this.io.read_child_enable <> next.io.output_prev_enable
    }

}