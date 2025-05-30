package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.mem._
import fpga.Const._
import fpga.pheap.Func._

// rank processing unit
class RPU (val level : Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new Token(level))
        val token_out = Output(new Token(level + 1))

        // read data from next level
        val read_next_out = Output(Bool())
        val read_next_addr_out = Output(UInt(addr_width(level + 1).W))
        val next_data_in = Input(UInt(pair_width(level + 1).W)) 

        // receive signals from prev RPU
        val read_in = Input(Bool())
        val read_addr_in = Input(UInt(addr_width(level).W))
        val data_out = Output(UInt(pair_width(level).W))    
    })

    // regs
    val data_reg = RegInit(0.U(pair_width(level).W))
    val token = RegInit(Token.default(level))
    val token_next = RegInit(Token.default(level + 1))

    // memory parameters
    val local_data_depth = data_depth(level)
    val local_data_width = pair_width(level)

    // different types of memory
    // sram/ffmem data width : 2 * Node(Pair)
    val memory : SinglePortMemoryImpl = mem_type match {
        case "SRAM" => Module(new SinglePortSram(local_data_depth, local_data_width))
        case "FFMEM" => Module(new SinglePortFFMem(local_data_depth, local_data_width))
        case _ => Module(new SinglePortSram(local_data_depth, local_data_width))
    } 

    // memory initialization
    memory.idle()

    // cmp inside RPU
    val local_cmp = Module(new Cmp(level))

    // cmp initialization
    local_cmp.io.token_in := token
    local_cmp.io.cur_pair_in := data_reg
    local_cmp.io.next_pair_in := io.next_data_in

    // IO initialization
    io.token_out := Token.default(level + 1)
    io.read_next_out := false.B
    io.read_next_addr_out := DontCare
    io.data_out := data_reg

    // auxiliary signals
    val ready = io.token_in.op.push.existing | io.token_in.op.pop
    val local_addr = WireInit(0.U(addr_width(level).W))

    // state
    val read :: cmp :: write :: Nil = Enum(3) 
    val state = RegInit(read)

    // FSM
    switch (state) {
        is (read) {
            when (ready) {
                // read next RPU
                io.read_next_out := true.B
                if (level == 1) {
                    io.read_next_addr_out := 0.U
                } else {
                    io.read_next_addr_out := io.token_in.position.tail(1)
                }
                // read current RPU
                if (level == 1 || level == 2) {
                    local_addr := 0.U
                } else {
                    local_addr := io.token_in.position.tail(1) >> 1
                }
                // update data
                data_reg := memory.read(local_addr)
                // update token
                token := io.token_in
                // transition
                state := cmp
            }
            when (io.read_in) { // next RPU -> prev RPU
                local_addr := io.read_addr_in
                data_reg := memory.read(local_addr)
            }
            // After 3 clocks : pass operation to next RPU
            io.token_out := token_next
            token_next := Token.default(level + 1) // update token_next
        }
        is (cmp) {
            // update regs by local_cmp results
            data_reg := local_cmp.io.pair_out
            token_next := local_cmp.io.token_out
            // transition
            state := write
        }
        is (write) {
            // write back to memory
            if (level == 1 || level == 2) {
                local_addr := 0.U
            } else {
                local_addr := token.position.tail(1) >> 1
            }
            memory.write(local_addr, data_reg)
            // transition
            state := read
        }
    }

    // connect all rpus
    def ~> (next : RPU) : Unit = {
        this.io.token_out <> next.io.token_in
        this.io.read_next_out <> next.io.read_in
        this.io.read_next_addr_out <> next.io.read_addr_in
        next.io.data_out <> this.io.next_data_in
    }

}