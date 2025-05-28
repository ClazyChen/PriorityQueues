package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Node._
import fpga.pheap.Func._

// rank processing unit
class RPU (val level : Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new Token(level)) 
        val token_out = Output(new Token(level + 1)) 

        // read data from next level
        val read_next_out = Output(Bool())
        val read_next_addr_out = Output(UInt(addr_width(level + 1).W))
        val next_data_in = Input(UInt(Pair.getWidth(level + 1).W))

        // receive signals from prev RPU
        val read_in = Input(Bool())
        val read_addr_in = Input(UInt(addr_width(level).W))
        val data_out = Output(UInt(Pair.getWidth(level).W))
    })
    
    val token = RegInit(Token.default(level))
    val token_next = RegInit(Token.default(level + 1))
    val data_reg = RegInit(Pair.default(level)) // update memory

    // 内嵌两个子模块
    val mem = Module(new MemBlock(level))
    val local_cmp = Module(new Cmp(level))

    // mem和RPU信号的连接
    io.read_in <> mem.io.read
    io.read_addr_in <> mem.io.read_addr
    io.data_out <> mem.io.pair_out

    // state
    val read :: cmp :: write :: Nil = Enum(3)
    val state = RegInit(read)

    // io初始化
    io.token_out := Token.default(level + 1)
    io.read_next_out := false.B
    io.read_next_addr_out := DontCare
    io.data_out := data_reg.asUInt

    // mem初始化
    mem.io.read := false.B
    mem.io.read_addr := DontCare
    mem.io.write := false.B
    mem.io.write_addr := DontCare
    mem.io.write_pair := DontCare
    
    // local_cmp初始化
    local_cmp.io.token_in := token
    local_cmp.io.node_in := mem.io.node_out.asTypeOf(new Node(level))
    local_cmp.io.cur_pair_in := mem.io.pair_out.asTypeOf(new Pair(level))
    local_cmp.io.next_pair_in := io.next_data_in.asTypeOf(new Pair(level + 1))
        
    // 状态信号，都是wire类型
    val ready = io.token_in.op.pop | io.token_in.op.push.existing // 新的操作到达

    // FSM
    switch(state) {
        is (read) {
            when (ready) {
                // 读本层
                mem.io.read := true.B
                mem.io.read_addr := io.token_in.position
                // 读下层
                io.read_next_out := true.B
                io.read_next_addr_out := io.token_in.position << 1.U
                // update token
                token := io.token_in
                // transition
                state := cmp
            }.otherwise {}
            // 三个周期结束后，传递操作给下一层RPU，此时本层已经没有ready信号
            io.token_out := token_next
            token_next := Token.default(level + 1) // 刷新token_next
        }
        is (cmp) {
            // update regs by local_cmp results
            data_reg := local_cmp.io.pair_out.asTypeOf(new Pair(level))
            token_next := local_cmp.io.token_out
            // transition
            state := write
        }
        is (write) {
            // write back to mem
            mem.io.write := true.B
            mem.io.write_addr := token.position
            mem.io.write_pair := data_reg.asUInt
            // transition
            state := read
        }
    }

    // connect all rpus
    def ~> (next : RPU) : Unit = {
        this.io.token_out <> next.io.token_in
        this.io.read_next_out <> next.io.read_in
        this.io.read_next_addr_out <> next.io.read_addr_in
        this.io.next_data_in <> next.io.data_out
    }

}