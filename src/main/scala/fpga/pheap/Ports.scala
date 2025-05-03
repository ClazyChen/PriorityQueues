package fpga.pheap

import chisel3._
import chisel3.util._

// we implement two types of memory in this repo
// 1. single port memory (1RW)
// 2. pseudo-dual-port memory (1R1W)

// The read port for pseudo-dual-port memory
class ReadPort(val addr_width: Int, val data_width: Int) extends Bundle {
  val en = Input(Bool())
  val addr = Input(UInt(addr_width.W))
  val data = Output(UInt(data_width.W))
}

// The write port of pseudo-dual-port memory
class WritePort(val addr_width: Int, val data_width: Int) extends Bundle {
  val en = Input(Bool())
  val addr = Input(UInt(addr_width.W))
  val data = Input(UInt(data_width.W))
}

// The read/write port of single port memory
class RWPort(val addr_width: Int, val data_width: Int) extends Bundle {
  val en = Input(Bool())
  val wen = Input(Bool())
  val addr = Input(UInt(addr_width.W))
  val data_in = Input(UInt(data_width.W))
  val data_out = Output(UInt(data_width.W))
}

class PHeapMemIO (val data_depth: Int, val data_width: Int) extends Bundle {
  val addr_width = log2Ceil(data_depth)

  val r = new ReadPort(addr_width, data_width)
  val w = new WritePort(addr_width, data_width)
}

trait PHeapMemTrait extends Module{
  val io: PHeapMemIO
}

object create_level_mem {
  def apply(count_of_levels: Int, mem_set: String): Seq[PHeapMemTrait]= {
    Seq.tabulate(count_of_levels) { idx =>
      val level = idx + 1
      val data_width = 1 << idx
      val node_width = WireDefault(BNode.default(level)).asUInt.getWidth

      val mem: PHeapMemTrait = if (mem_set == "Sram") {
        Module(new Sram(data_width, node_width))
      } else {
        Module(new FFMem(data_width, node_width))
      }
      mem
    }
  }
}