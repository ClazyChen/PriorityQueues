package fpga.heap

import chisel3._
import chisel3.util._
import fpga._
import fpga.heap.Const._

object capacity_width {
  def apply(level: Int): Int = {
    val max_capacity = count_of_levels - level + 1
    max_capacity
  }
}

// B[i].active: indicate this node is active or not
// B[i].value: if the node is active, this field holds the actual priority value
// B[i].capacity: this field contains the number of inactive node in this subtree
class BNode(val level: Int) extends Bundle {
  val entry = new Entry
  val capacity = UInt(capacity_width(level).W)
  def < (that: BNode): Bool = (this.entry < that.entry) || !that.entry.existing
}
object BNode {
  def default(level: Int): BNode = {
    val bnode = Wire(new BNode(level))
    bnode.entry := Entry.default
    bnode.capacity := (1 << (count_of_levels - level + 1)- 1).U
    bnode
  }
  def last: BNode = {
    val bnode = Wire(new BNode(count_of_levels))
    bnode.capacity := 0.U
    bnode
  }
}

