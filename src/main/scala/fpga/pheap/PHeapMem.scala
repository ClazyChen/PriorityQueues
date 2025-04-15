package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.pheap.Const._


class PHeapMem(val data_depth: Int, val mem_impl: String) extends Module {
  val addr_width = log2Ceil(data_depth)
  val io = IO(new Bundle {
    val r = new ReadPort(addr_width, data_width)
    val w = new WritePort(addr_width, data_width)
  })
  val mem = mem_impl match {
    case "FFMem" => Module(new FFMem(data_depth, data_width))
    case "MemImpl"  => Module(new Sram(data_depth, data_width))
  }
  mem_impl match {
    case "FFMem" =>
      val ffmem = Module(new FFMem(data_depth, data_width))
      ffmem.io.r.en := io.r.en
      ffmem.io.r.addr := io.r.addr
      io.r.data := ffmem.io.r.data
      ffmem.io.w.en := io.w.en
      ffmem.io.w.addr := io.w.addr
      ffmem.io.w.data := io.w.data
    case "SRAM" =>
      val sram = Module(new Sram(data_depth, data_width))
      sram.io.r.en := io.r.en
      sram.io.r.addr := io.r.addr
      io.r.data := sram.io.r.data

      sram.io.w.en := io.w.en
      sram.io.w.addr := io.w.addr
      sram.io.w.data := io.w.data
  }

  def read_block(position: UInt, level: Int): BNode = {
    io.r.en   := true.B
    io.r.addr := position
    // 这里将读出数据转换为 BNode 类型
    io.r.data.asTypeOf(new BNode(level))
  }

  def write_block(position: UInt, bnode: BNode): Unit = {
    io.r.en   := true.B
    io.r.addr := position
    io.r.data := bnode.asUInt
  }
}

object bheap_mem_ops {
  def connect(this_mem: PHeapMem, that_mem: PHeapMem): Unit = {
    // 读端口
    this_mem.io.r.en := that_mem.io.r.en
    this_mem.io.r.addr := that_mem.io.r.addr
    that_mem.io.r.data := this_mem.io.r.data

    // 写端口
    this_mem.io.w.en := that_mem.io.w.en
    this_mem.io.w.addr := that_mem.io.w.addr
    this_mem.io.w.data := that_mem.io.w.data
  }
}

