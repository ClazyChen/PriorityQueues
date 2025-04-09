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
    val memory = Module(new Memory(data_depth, rank_width, use_sram = true))
    val token = RegInit(TokenNode.default)

    token := token_in
    token_out := token
    mem_out := memory

    def ~>(next: RPU) = {
        next.io.token_in := this.io.token_out
        this.io.mem_in := next.io.mem_out
    }

    def init_memory(): Unit = {
        for(i <- 0 until get_data_depth(level)) {
            val node = Node.init(level, 0)
            write(memory, i, node)
        }
    }

    val addr = token_in.position

    // 操作序列这里不做控制,交给上层处理
    when (!io.token_in.op.pop) {
        val stored_node = read(memory, addr)

        when (!stored_node.value.existing) {
            val newNode = read(memory, addr)
            newNode.capacity -= 1
            write(memory, addr, newNode)
            token.op = Operator.nop
        } .elsewise (stored_node.value < token.entry) {
            val newNode = Wire(Node.init(level, token.entry))
            write(memory, addr, newNode)
            token.entry := stored_node.value
        } .otherwise { }

        val lc_pos = get_lc_pos(level, token.position)
        val rc_pos = lc_pos + 1
        val lc_node = read(mem_in, lc_pos)
        token.position := Mux(lc_node.capacity > 0, lc_pos, rc_pos)
        
    } .otherwise {

    }
}
