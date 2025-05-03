package fpga.pheap

import chisel3._
import chisel3.util._

// a pseudo-dual-port memory with SRAM
class Sram(val data_depth: Int, val data_width: Int) extends Module with PHeapMemTrait {
  val addr_width = log2Ceil(data_depth)

  val level = addr_width + 1
  val default_flat: UInt = BNode.default(level).asUInt

  val io = IO(new PHeapMemIO(data_depth, data_width))

  val mem = SyncReadMem(data_depth, UInt(data_width.W))

  when (reset.asBool) {
    for (i <- 0 until data_depth) {
      mem.write(i.U, default_flat)
    }
  }

  io.r.data := DontCare

  // SRAM read has a one cycle delay
  when (io.r.en) {
    io.r.data := mem.read(io.r.addr)
  }

  when (io.w.en) {
    mem.write(io.w.addr, io.w.data)
  }

  // read and write conflict handling: when the read and write address are the same, we need to handle it specially
  // because the SRAM read has a one cycle delay, we need to store the write information of the previous cycle to handle the conflict
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
