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
    val token_next = RegInit(Token.default(level))
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
    val cmp_lc_rc = io.pair_in.left_node < io.pair_in.right_node
    val cmp_input_lc = token.op.push < io.pair_in.left_node
    val cmp_input_rc = token.op.push < io.pair_in.right_node
    val swap_enable = token.op.push < mem.io.node_out

    // 状态寄存器
    val rCycle0 :: rCycle1 :: rCycle2 :: Nil = Enum(3)
    val state = RegInit(rCycle0)
    val token_next = RegInit(Node.default)

    // 封装
    def read_node (position : UInt) : Unit = {
        mem.read_node := true.B
        mem.position_in := get_local_index(position, level)
    }
    def read_children (position : UInt) : Unit = {
        io.read_children_out := true.B
        io.position_out := get_lc_local_index(position, level)
    }
    def write_node (node : Node, position : UInt) : Unit = {
        mem.io.write := true.B
        mem.io.node_in := node
        mem.io.position_in := position
    }
    def write_children (node : Node, position : UInt) : Unit = {
        io.write_out := ture.B
        io.node_out := node
        io.position_out := position
    }

    // FSM
    switch (state) {
        is (rCycle0) {
            when (io.token_in.position) {
                token := io.token_in // 读入token
                val operator = generate_op(io.token_in)
                switch (operator) {
                    is (State.enq) {
                        read_node(io.token_in.position)
                        read_children(io.token_in.position)
                    }
                    is (State.deq) {
                        read_children(io.token_in.position)
                    }
                    is (State.edq) { // repalce
                        read_node(io.token_in.position)
                        read_children(io.token_in.position)
                    }
                    is (State.nop) {}
                } 
                // 进入下一周期
                state := rCycle1 
            }
            .otherwise {}
        }
        is (rCycle1) { // keep signals
            val operator = generate_op(token) // token already update
            switch (operator) {
                is (State.enq) {
                    read_node(token.position)
                    read_children(token.position)
                }
                is (State.deq) {
                    read_children(token.position)
                }
                is (State.edq) { // repalce
                    read_node(token.position)
                    read_children(token.position)
                }
                is (State.nop) {}
            }
            // 进入下一周期
            state := rCycle2 
        }
        is (rCycle2) {
            // 状态转移 实现enqueue dequeue edq
            val operator = generate_op(token)
            switch (operator) {
                is (State.enq) {
                    when (is_empty(mem.io.node_out)) {
                        val write_current_node = Node.generate(level, token.op.push, mem.io.node_out.capacity - 1.U)
                        write_node(write_current_node, token.position)
                        token_next := Token.default(level)
                        io.token_out := token_next
                        token := Token.default(level)
                    }.otherwise { // not empty -> swap
                        when (swap_enable) {
                            val write_current_node = Node.generate(level, token.op.push, mem.io.node_out.capacity)
                            write_node(write_current_node, token.position)
                            val token_op = new Token(level + 1)
                            token_op.push := mem.io.node_out.value
                            token_op.pop := token.op.pop
                            val next_rpu_position = Mux(is_capacity_valid(io.pair_in.left),
                            get_lc_global_index(token.position), get_rc_global_index(token.position))
                            token_next := Token.generate(level + 1, token_op, next_rpu_position)
                            io.token_out := token_next
                            token := Token.default(level)
                        }.otherwise {
                            val next_rpu_position = Mux(is_capacity_valid(io.pair_in.left),
                            get_lc_global_index(token.position), get_rc_global_index(token.position))
                            token_next := Token.generate(level + 1, token.op, next_rpu_position)
                            io.token_out := token_next
                            token := Token.default(level)
                        }
                    }
                }
                is (State.deq) {
                    // write binary heap
                    val lower_rank_position = Mux(cmp_lc_rc, get_lc_global_index(token.position),
                     get_rc_global_index(token.position))
                    val lower_node = Mux(cmp_lc_rc, io.pair_in.left, io.pair_in.right)
                    val write_current_node = Node.generate(level, lower_node, mem.io.node_out.capacity + 1.U)
                    write_node(write_current_node, token.position)
                    val write_next_node = Node.generate(level + 1, Node.default.value, lower_node.capacity + 1.U)
                    write_children(write_next_node, lower_rank_position)
                    // pass token
                    token_next := Token.generate(level + 1, token.op, lower_rank_position)
                    io.token_out := token_next
                    token := Token.default(level)
                }
                is (State.edq) { // repalce
                    // write binary heap
                    when (token.position === 1.U) {
                        val write_current_node = Node.generate(level, token.op.push, mem.io.node_out.capacity)
                        write_node(write_current_node, token.position)
                        // pass token
                        val next_token = new Token(level + 1)
                        next_token.op.pop := token.pop
                        next_token.op.push.existing := false.B
                        next_token.op.push.rank := 0.U(position_width(level + 1.U))
                        next_token.op.push.metadata := 0.U(token.position.W)
                        token_next := next_token
                        io.token_out := token_next
                        token := Token.default(level)
                    }.otherwise {}
                }
                is (State.nop) {}
            }
            state := rCycle0
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