package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._
import fpga.mem._

/*
input->read->cmp->write
input: 外界给出输入信号
input->read：写入token和read enable
read: 根据token生成读信号和读地址
read->cmp：读出父节点和子节点保存在read模块的寄存器中，并传递给cmp
cmp: 比较，并生成下个RPU的token和更新的节点
cmp->write：cmp模块的寄存器写入next token，和update dual node
write: 收到cmp更新的节点，生成写信号、写地址、写数据

Q:
上游给的操作信号能保证是周期一开始时就到达吗?

看移位寄存器测试的波形图，是下降沿才产生的操作信号，对于移位寄存器来说，
收到输入信号后马上开始比较，然后上升沿写入。
相当于用半个周期完成了比较等操作，而不是一个周期，因为上游的信号没及时给出。

收到信号的那个周期算第一个周期还是下个周期才算第一个周期呢？

因为怕不是周期一开始就收到操作信号，如果在该周期就进行read操作可能会导致时钟周期变长
也有可能测试的波形图是为了直观看清输入信号才在下降沿给出信号的，导致我理解有误
上面的input和read可能可以合并在一个周期
*/

// 与pheap无关的Q: 
// 时钟周期是如何计算出来的
// 写SRAM需要的时间可以计算吗
// 怎么让写SRAM变成两个周期

// a RPU in the PHeap, one RPU per level
class RPU(val level : Int, use_mem : String) extends Module {
    val io = IO(new Bundle {
        val enable_in       = Input(Bool()) // 启动信号
        val enable_out      = Output(Bool()) // 传递给下个RPU的激活信号
        val token_in        = Input(new Token(level)) // from the previous RPU
        val next_token_out  = Output(new Token(level + 1)) // to the next RPU

        // pop/replace的输出
        // 可选端口：只有 level == 1 时需要
        val entry_out = if (level == 1) Some(Output(new Entry)) else None

        // 因为需要操作两个SRAM，即使SRAM集成在RPU里，也要拉条线到下个RPU里读
        // 不过感觉不优雅

        // 读下层，一次性读两个子节点，第level+1层有2^level个节点，所以地址线需要level-1根
        val read_next_out       = Output(Bool())
        val read_next_addr_out  = Output(UInt( addr_width(level + 1).W ))
        val read_dual_node_in   = Input(UInt(rw_width(level + 1).W)) 

        // 被上层读
        val read_in             = Input(Bool())
        val read_addr_in        = Input(UInt( addr_width(level).W ))
        val read_dual_node_out  = Output(UInt(rw_width(level).W))         
    })

    val read  = Module(new Read(level))
    val cmp   = Module(new Cmp(level))
    val write = Module(new Write(level))

    val depth = data_depth(level)
    val width = rw_width(level)

    val mem : SinglePortMemoryImpl = use_mem match {
        case "SRAM"  => Module(new SinglePortSram(depth, width))
        case "FFMEM" => Module(new SinglePortFFMem(depth, width))
        case _       => Module(new SinglePortSram(depth, width))
    } 
    val port = mem.getIO

    // 读：本层和上一层都可能读
    val read_addr            = Mux(read.io.read, read.io.read_addr, io.read_addr_in)
    port.en                 := read.io.read | io.read_in
    port.wen                := write.io.write
    port.addr               := Mux(write.io.write, write.io.write_addr, read_addr)
    port.data_in            := write.io.write_dual_node
    read.io.read_dual_node  := port.data_out
    io.read_dual_node_out   := port.data_out


    // 多个模块都要用到，所以放外面，而且这个寄存器写入时间和read的写入时间不一致
    val token = RegInit(Token.default(level))
    when (io.enable_in) {
        token := io.token_in
    }

    // 收到启动信号的下个周期才启动read，所以用个寄存器延迟一拍
    val enable = RegNext(io.enable_in)

    // 连线
    // read
    read.io.enable_in           := enable
    read.io.position_in         := token.position
    io.read_next_out            := read.io.read_next
    io.read_next_addr_out       := read.io.read_next_addr
    read.io.read_next_dual_node := io.read_dual_node_in

    // cmp
    cmp.io.enable_in         := read.io.enable_out
    cmp.io.token_in          := token
    cmp.io.dual_node_in      := read.io.dual_node_out
    cmp.io.next_dual_node_in := read.io.next_dual_node_out
    // 第三个周期要提供下个RPU的输入了
    // input read cmp write
    //                input read cmp write
    io.next_token_out        := cmp.io.next_token_out
    io.enable_out            := Mux(cmp.io.done_out, false.B, cmp.io.enable_out)
    io.entry_out match {
        case Some(entry_out) => entry_out := cmp.io.entry_out.getOrElse(DontCare)
        case None            =>
    }

    // write
    write.io.enable_in           := cmp.io.enable_out
    write.io.position_in         := token.position
    write.io.update_dual_node_in := cmp.io.update_dual_node_out
    
    // connect RPUs, this ~> next
    def ~>(next: RPU) = {
        next.io.enable_in         := this.io.enable_out
        next.io.token_in          := this.io.next_token_out
        next.io.read_in           := this.io.read_next_out
        next.io.read_addr_in      := this.io.read_next_addr_out
        this.io.read_dual_node_in := next.io.read_dual_node_out
    }
}