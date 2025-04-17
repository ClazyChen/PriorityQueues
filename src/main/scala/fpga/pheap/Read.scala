package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._
import fpga.mem._

class Read(val level : Int) extends Module {
    val io = IO(new Bundle {
        val enable_in   = Input(Bool())
        val enable_out  = Output(Bool())
        val position_in = Input(UInt(position_width(level).W))
        // read parent
        val read            = Output(Bool()) 
        val read_addr       = Output(UInt((level - 1).W))
        val read_dual_node  = Input(UInt(rw_width(level).W)) 
        val dual_node_out   = Output(UInt(rw_width(level).W)) 
        // read children
        val read_next           = Output(Bool())
        val read_next_addr      = Output(UInt((level).W))
        val read_next_dual_node = Input(UInt(rw_width(level + 1).W)) 
        val next_dual_node_out  = Output(UInt(rw_width(level + 1).W)) 
    })

    // 延迟一拍，启动下个模块
    val enable     = RegNext(io.enable_in) 
    io.enable_out := enable 

    // 读地址，两个node一起读
    // 父节点地址，position去掉高1位再右移1位
    if (level <= 2) {
        io.read_addr := 0.U
    } else {
        io.read_addr := io.position_in.tail(1) >> 1
    }

    // 子节点地址，position去掉高1位就行
    if (level == 1) {
        io.read_next_addr := 0.U
    } else {
        io.read_next_addr := io.position_in.tail(1)
    }

    // 读信号
    io.read      := io.enable_in 
    io.read_next := io.enable_in 

    // 保存读出数据，传给下一模块
    val dual_node     = RegNext(io.read_dual_node)
    io.dual_node_out := dual_node

    val next_dual_node     = RegNext(io.read_next_dual_node) 
    io.next_dual_node_out := next_dual_node
}