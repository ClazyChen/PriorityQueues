package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Entry
import fpga.pheap.Const._

object capacity_width {
  def apply(level: Int): Int = {
    val max_capacity = (1 << (count_of_levels - level + 1))
    log2Ceil(max_capacity)
  }

}

// B[i].active: indicate this node is active or not.
// B[i].value: if the node is active, this field holds the actual priority value.
// B[i].capacity: this field contains the number of inactive node in this subtree.
class BNode(val level: Int) extends Bundle {
  val entry = new Entry // B[i].value :: B[i].active
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
    bnode.capacity := 0.U
    bnode
  }
}

// T[i].operation: this field holds an instruction.
// T[i].value: this field may hold a priority value that needs to be inserted into B.
// T[i].position: this field can hold the index of a node at level.
class TNode(val level: Int) extends Bundle {
  val operation = new Operator
  val value = new Entry
  // TODO: 需要调整位宽
  val position = UInt(count_of_levels.W)
}

object TNode {
  def default(level: Int): TNode = {
    val tnode = Wire(new TNode(level))
    tnode.operation := Operator.nop
    tnode.value := Entry.default
    tnode.position := 1.U
    tnode
  }
}

object TreeIndexing {

  // 计算给定 count_of_levels 下的总节点数（满二叉树）
  def total_node_count: Int = (1 << count_of_levels) - 1

  // 根据索引 i 获取节点所处的层级（从 1 开始）
  def get_level_from_index(index: Int): Int = log2Ceil(index + 2)

  // 给定层级和在该层的 offset（从 0 开始）返回对应的索引（从 0 开始）
  def get_index_from_level(level: Int, offset: Int): Int = (1 << (level - 1)) - 1 + offset

  // 给定层级返回该层包含的节点数量
  def get_nodes_at_level(level: Int): Int = 1 << (level - 1)

  // 给定层级返回起始索引
  def level_start_index(level: Int): Int = (1 << (level - 1)) - 1

  def get_level_start_index(level: Int): Int = (1 << (level - 1))

  def get_lc_pos(level: Int, offset: UInt): UInt = ((get_level_start_index(level).U + offset - 1.U) * 2.U) - get_level_start_index(level + 1).U + 1.U

  def get_rc_pos(level: Int, offset: UInt): UInt = ((get_level_start_index(level).U + offset - 1.U) * 2.U) - get_level_start_index(level + 1).U + 2.U

}

class BNode_read_port(level: Int) extends Bundle {
  val en = Input(Bool())
  val addr = Input(UInt(count_of_levels.W)) // 每层 2^(level-1) 个节点
  val data = Output(new BNode(level))
}

class BNode_write_port(level: Int) extends Bundle {
  val addr = Input(UInt(count_of_levels.W))
  val data = Input(new BNode(level))
  val en   = Input(Bool())
}
