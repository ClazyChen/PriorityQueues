package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.pheap.Const._

// implement the io interface of each level in PHeap
class PHeapLevelMemIO(val level: Int, val data_width: Int) extends Bundle {
  val r = new ReadPort(level, data_width)
  val w = new WritePort(level, data_width)
}

// implement each level memory in PHeap
class PHeapLevelMem(val level: Int, val mem_impl: String) extends Module with DualPortMemoryImpl {
  val addr_width = if(level <= 1) 1 else  log2Ceil(level)

  val io = IO(new PHeapLevelMemIO(level, data_width))

  val mem = SyncReadMem(level, UInt(data_width.W))

  io.r.data := DontCare
  when(io.r.en) {
    io.r.data := mem.read(io.r.addr)
  }
  when(io.w.en) {
    mem.write(io.w.addr, io.w.data)
  }
  val same_addr_delay = RegNext(io.r.en && io.w.en && io.r.addr === io.w.addr)
  val wdata_delay = RegNext(io.w.data)

  // when the read and write address are the same, output the written data
  when (same_addr_delay) {
    io.r.data := wdata_delay
  }
  // provide the IO interface
  def getRPort: ReadPort = io.r
  def getWPort: WritePort = io.w
}