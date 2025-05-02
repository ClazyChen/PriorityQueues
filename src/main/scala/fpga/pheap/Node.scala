package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Entry
import fpga.pheap.Const._

// TODO: capacity宽度问题
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
  val capacity = UInt(count_of_levels.W)
  def < (that: BNode): Bool = (this.entry < that.entry) || !that.entry.existing
}

object BNode {
  def default(level: Int): BNode = {
    val bnode = Wire(new BNode(level))
    bnode.entry := Entry.default
    bnode.capacity := ((1 << (count_of_levels - level + 1)) - 1).U
    bnode
  }
  def last: BNode = {
    val bnode = Wire(new BNode(count_of_levels))
    bnode.entry := Entry.default
    bnode.capacity := 1.U
    bnode
  }
}

// T[i].operation: this field holds an instruction
// T[i].value: this field may hold a priority value that needs to be inserted into B
// T[i].position: this field can hold the index of a node at level
class TNode(val level: Int) extends Bundle {
  val operation = new Operator
  val value = new Entry
  val position = UInt(count_of_levels.W)
}

object TNode {
  def default(level: Int): TNode = {
    val tnode = Wire(new TNode(level))
    tnode.operation := Operator.nop
    tnode.value := Entry.default
    tnode.position := 0.U
    tnode
  }
}


object TreeIndexing {

  // 总层数 -> 总节点数
  def total_node_count: Int = ((1 << count_of_levels) - 1)

  // 全局索引i -> 层级
  def get_level_from_index(index: Int): Int = log2Ceil(index + 2)

  // 层内索引i + 层索引level -> 全局索引
  def get_index_from_level(level: Int, offset: UInt): UInt = ((1 << (level - 1))- 1).U + offset

  // 层级 -> 该层起始索引（实际）
  def level_start_index(level: Int): Int = (1 << (level - 1)) - 1

  // 层级 -> 该层的节点数量
  def get_nodes_at_level(level: Int): Int = 1 << (level - 1)

  // 层级 -> 该层起始索引
  def get_level_start_index(level: Int): UInt = (1 << (level - 1)).U

  // 父节点层内索引 + 层级 -> 左子节点层内索引
  def get_lc_pos(level: Int, offset: UInt): UInt = ((get_level_start_index(level) + offset - 1.U) * 2.U) - get_level_start_index(level+1) + 1.U

  // 父节点层内索引 -> 右子节点层内索引
  def get_rc_pos(level: Int, offset: UInt): UInt = ((get_level_start_index(level) + offset - 1.U) * 2.U + 1.U) - get_level_start_index(level+1) + 1.U
}
