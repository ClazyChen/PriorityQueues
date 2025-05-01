package fpga.pheap

import chisel3._
import chisel3.util._

// a pseudo-dual-port memory with flip-flop
class LevelMem(val data_depth: Int, val data_width: Int) extends Module with DualPortMemoryImpl {
  val addr_width = log2Ceil(data_depth)
  val level = addr_width + 1
  val default_flat: UInt = BNode.default(level).asUInt

  val io = IO(new Bundle{
    val r = new ReadPort(addr_width, data_width)
    val w = new WritePort(addr_width, data_width)
  })

  val mem: Vec[UInt] = RegInit(
    VecInit(Seq.fill(data_depth)(default_flat))
  )

  io.r.data := DontCare

  when(io.r.en) {
    io.r.data := mem(io.r.addr)
  }
  when(io.w.en) {
    mem(io.w.addr) := io.w.data
  }
  // handle the read and write conflict
  when (io.r.en && io.w.en && io.r.addr === io.w.addr) {
    io.r.data := io.w.data
  }
  // provide the IO interface
  def getRPort: ReadPort = io.r
  def getWPort: WritePort = io.w
}
