package fpga

import chisel3._
import chisel3.util._
import fpga.Const._

// definite the entry in the priority queue
// rank = 0 is the highest priority
// rank = 2^rank_width - 1 is the lowest priority
class Entry extends Bundle {
  // 定义条目是否存在
  val existing = Bool()
  // 定义条目的优先级，位宽为 priorityWidth
  val rank = UInt(rank_width.W)
  // 定义条目的流标号，位宽为 flowWidth
  val metadata = UInt(metadata_width.W)


  def <(that: Entry): Bool = (this.rank < that.rank) || !that.existing
}

// the default entry
object Entry {
  def default: Entry = {
    val entry = Wire(new Entry)
    entry.existing := false.B
    entry.metadata := 0.U(metadata_width.W)
    entry.rank := -1.S(rank_width.W).asUInt
    entry
  }
}


// an operator for the priority queue
class Operator extends Bundle {
  val push    = Bool()
  val pop     = Bool()
  val replace = Bool()

  val entry_in = Input(new Entry)
}

// the io of the priority queue
trait PriorityQueueTrait extends Module {
  class PQIO extends Bundle {
    val op_in     = Input(new Operator)
    val entry_out = Output(new Entry)
  }
  val io: PQIO
}