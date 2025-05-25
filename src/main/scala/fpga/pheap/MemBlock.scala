package fpga.pheap

import scala._
import chisel3._
import fpga._
import chisel3.util._
import fpga.mem._
import fpga.pheap.Func._
import fpga.pheap.Node._

// RPU -> memory
class MemBlock (val level : Int) extends Module { 

    val local_data_depth = data_depth(level) // number of nodes
    val local_data_width = Pair.getWidth(level) // data width

    // MemBlock IO
    val io = IO(new Bundle {
        // 读memory中的数据
        val read = Input(Bool())
        val read_addr = Input(UInt(addr_width(level).W))
        // 写信号
        val write = Input(Bool())
        val write_addr = Input(UInt(addr_width(level).W))
        val write_pair = Input(UInt(Pair.getWidth(level).W))
        // 返回信号
        val pair_out = Output(UInt(Pair.getWidth(level).W))
        val node_out = Output(UInt(Node.getWidth(level).W))
    })

    // different type of memory
    // sram/ffmem的数据线宽度为2 * Node(Pair)
    val memory = mem_type match {
        case "SRAM"  => Module(new SinglePortSram(local_data_depth, local_data_width))
        case "FFMEM" => Module(new SinglePortFFMem(local_data_depth, local_data_width))
        case _  => Module(new SinglePortSram(local_data_depth, local_data_width)) // 默认使用单端口SRAM
    } 
    
    // memory初始化
    memory.idle()

    // reg
    val pair_reg = RegInit(Pair.default(level))
    val select_node = RegInit(false.B) // 0 -> left  1 -> right

    // wire类型，控制信号
    val local_addr = WireInit(0.U(addr_width(level).W))
    
    // 输出信号
    io.node_out := Mux(select_node, pair_reg.left_node.asUInt, pair_reg.right_node.asUInt)
    io.pair_out := pair_reg.asUInt

    // read from memory
    when (io.read) {
        when (io.read_addr === 1.U) {
            local_addr := 0.U
            select_node := false.B
        }.otherwise {
            local_addr := Mux(io.read_addr(0), g2l(io.read_addr, level) - 1.U
            , g2l(io.read_addr, level)) // 根据position末位来判断选哪个node
            select_node := Mux(io.read_addr(0), true.B, false.B) 
        }   
        pair_reg := memory.read(local_addr).asTypeOf(new Pair(level))
    }.otherwise {}

    // write to memory
    when (io.write) {
        when (io.read_addr === 1.U) {
            local_addr := 0.U
        }.otherwise {
            local_addr := Mux(io.read_addr(0), g2l(io.read_addr, level) - 1.U
            , g2l(io.read_addr, level)) // 根据position末位来判断选哪个node
        } 
        memory.write(local_addr, io.write_pair.asUInt)
    }.otherwise {}

}