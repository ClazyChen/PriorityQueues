package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.Const._


// TODO 检查token,mem的时序问题
class RPU(val level: Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new TokenNode)
        val token_out = Output(new TokenNode)
        val mem_in = Input(new Memory(get_data_depth(level + 1)))
        val mem_out = Output(new Memory(get_data_depth(level)))
    })
    val memory = Module(new Memory(get_data_depth(level)))
    val token = RegInit(TokenNode.default)

    token := token_in
    // io.token_out := token
    io.mem_out := memory
    io.token_out := TokenNode.default

    // 怎么保证操作完成后再将token向下传递? FSM?
    val sCycle0 :: sCycle1 :: sCycle2 :: sIdle :: Nil = Enum(4)
    val state = RegInit(sIdle)

    switch(state) {
        is(sIdle) {
            when(io.token_in.op =/= Operator.nop) {
                token := io.token_in  
                state := sCycle0
            }
            io.token_out := TokenNode.default 
        }
        is(sCycle0) {
            io.token_out := TokenNode.default 
            state := sCycle1
        }
        is(sCycle1) {
            io.token_out := TokenNode.default 
            state := sCycle2
        }
        is(sCycle2) {
            io.token_out := token            
            state := sIdle                   
        }
    }

    val addr = token_in.position

    // 操作序列这里不做控制,交给上层处理
    when (!io.token_in.op.pop) {
        val stored_node = read(level, memory, addr)

        when (!stored_node.value.existing) {
            val newNode = Wire(Node.init(level, io.token_in.entry))
            newNode.capacity := newNode.capacity - 1.U
            write(level, memory, addr, newNode)
            token.op = Operator.nop
        } .elsewhen (stored_node.value < io.token_in.entry) {
            write(level, memory, addr, Node.init(level, io.token_in.entry))
            token.entry := stored_node.value
        } 

        val lc_pos = get_lc_pos(level, io.token_in.entry)
        val rc_pos = lc_pos + 1.U
        val lc_node = read(level + 1, mem_in, lc_pos)
        token.position := Mux(lc_node.capacity > 0, lc_pos, rc_pos)
        
    } .otherwise {

    }

    def ~>(next: RPU) = {
        next.io.token_in := this.io.token_out
        this.io.mem_in := next.io.mem_out
    }

    def init_memory(): Unit = {
        for(i <- 0 until get_data_depth(level)) {
            val node = Node.init(level, 0)
            write(level, memory, i, node)
        }
    }
}
