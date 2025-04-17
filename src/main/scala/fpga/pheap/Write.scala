package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._
import fpga.mem._

class Write(val level : Int) extends Module {
    val io = IO(new Bundle {
        val enable_in           = Input(Bool())
        val position_in         = Input(UInt(position_width(level).W))
        val write               = Output(Bool()) 
        val write_addr          = Output(UInt(addr_width(level).W))
        val write_dual_node     = Output(UInt(rw_width(level).W)) 
        val update_dual_node_in = Input(UInt(rw_width(level).W))
    })

    if (level <= 2) {
        io.write_addr := 0.U
    } else {
        io.write_addr := io.position_in.tail(1) >> 1
    }

    io.write           := io.enable_in 
    io.write_dual_node := io.update_dual_node_in
}