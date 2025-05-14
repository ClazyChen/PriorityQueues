package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._
import fpga.mem._


// a RPU in the PHeap, one RPU per level
class RPU(val level : Int, use_mem : String) extends Module {
    val io = IO(new Bundle {
        val token_in        = Input(new Token(level))
        val next_token_out  = Output(new Token(level + 1))

        // 读下层，一次性读两个子节点，第level+1层有2^level个节点，所以地址线需要level-1根
        val read_next_out       = Output(Bool())
        val read_next_addr_out  = Output(UInt( addr_width(level + 1).W ))
        val next_data_in        = Input(UInt(rw_width(level + 1).W)) 

        // 被上层读
        val read_in             = Input(Bool())
        val read_addr_in        = Input(UInt( addr_width(level).W ))
        val data_out            = Output(UInt(rw_width(level).W))    
    })

    // 定义状态
    val read :: cmp :: write :: Nil = Enum(3) 
    val state = RegInit(read)

    // mem
    val depth = data_depth(level)
    val width = rw_width(level)

    val mem : SinglePortMemoryImpl = use_mem match {
        case "SRAM"  => Module(new SinglePortSram(depth, width))
        case "FFMEM" => Module(new SinglePortFFMem(depth, width))
        case _       => Module(new SinglePortSram(depth, width))
    } 
    mem.idle() // 默认值

    // reg
    val data = RegInit(0.U(rw_width(level).W))
    val token = RegInit(Token.default(level))
    val next_token = RegInit(Token.default(level + 1))

    // 根据操作是否有效
    val start = io.token_in.op.push.existing | io.token_in.op.pop
    val addr = WireInit(0.U(addr_width(level).W))

    val cmp_m = Module(new Cmp(level))
    cmp_m.io.token_in     := token
    cmp_m.io.data_in      := data
    cmp_m.io.next_data_in := io.next_data_in

    // 默认输出
    io.read_next_out        := false.B
    io.read_next_addr_out   := DontCare
    io.data_out             := data
    io.next_token_out       := Token.default(level + 1)

    switch(state) {
        // 收到输入信号的当个周期就开始读了，所以read状态
        // 就相当于idle状态，根据输入确定是否读
        is(read) {
            // 第一个周期
            when(start) {
                // 激活下层读子节点
                io.read_next_out := true.B
                if (level == 1) {
                    io.read_next_addr_out := 0.U
                } else {
                    io.read_next_addr_out := io.token_in.position.tail(1)
                }

                // 读本层父节点（可以省略，因为上层执行的时候读过一次本层节点了）
                if (level <= 2) {
                    addr := 0.U
                } else {
                    addr := io.token_in.position.tail(1) >> 1
                }

                // 写入寄存器，切换到cmp状态进行处理
                data := mem.read(addr)
                token := io.token_in
                state := cmp
            }

            // 读给上层，状态不变
            when (io.read_in) {
                addr := io.read_addr_in
                data := mem.read(addr)
            }    

            // 第四个周期，输出token给下层并重置该寄存器
            // level i    : read cmp  write read read read...
            // level i + 1: read read read  read cmp  write...
            io.next_token_out := next_token
            next_token := Token.default(level + 1)
        }

        is(cmp) {
            // 比较后，更新父节点以及下一层的token
            data := cmp_m.io.update_data
            next_token := cmp_m.io.next_token_out
            state := write
        }

        is(write) {
            // 写入父节点
            if (level <= 2) {
                addr := 0.U
            } else {
                addr := token.position.tail(1) >> 1
            }
            mem.write(addr, data)
            state := read
        }
    }
    
    // connect RPUs, this ~> next
    def ~>(next: RPU) = {
        next.io.token_in          := this.io.next_token_out
        next.io.read_in           := this.io.read_next_out
        next.io.read_addr_in      := this.io.read_next_addr_out
        this.io.next_data_in      := next.io.data_out
    }
}