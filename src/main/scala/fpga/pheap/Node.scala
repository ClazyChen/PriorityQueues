package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Entry
import fpga.pheap.Const._

object capacity_width {
  def apply(level: Int): Int = {
    val max_nodes = (1 << (count_of_levels - level + 1)) - 1
    log2Ceil(max_nodes + 1)
  }
}

// 将二叉堆相关的索引操作集中在一个 object 里
object bheap_index_ops {
  // 将局部索引转换为全局索引
  def local_to_global(local_index: UInt, level: Int): UInt = {
    val base: UInt = (1 << (level - 1)).U
    base + local_index - 1.U
  }
  // 从全局的索引转化为某层的索引
  def global_to_local(global_index: UInt, level: Int): UInt = {
    global_index - ((1 << (level - 1)).U) + 1.U
  }
  // 获取左孩子在下一层的局部索引
//  def get_lc_pos(parent_local_index: UInt, level: Int): UInt = {
//    val parent_global: UInt = local_to_global(parent_local_index, level)
//    global_to_local(parent_global * 2.U, level + 1)
//  }
  def get_lc_pos(parent_local_index: UInt, level: Int): UInt = {
    parent_local_index
  }
  // 获取右孩子在下一层的局部索引
//  def get_rc_pos(parent_local_index: UInt, level: Int): UInt = {
//    val parent_global: UInt = local_to_global(parent_local_index, level)
//    global_to_local(parent_global * 2.U + 1.U, level + 1)
//  }
  def get_rc_pos(parent_local_index: UInt, level: Int): UInt = {
    parent_local_index + 1.U
  }
  def index_to_level(i: Int): Int = {
    val level = math.floor(math.log(i) / math.log(2)).toInt + 1
    level
  }

}

// B[i].active: indicate this node is active or not.
// B[i].value: if the node is active, this field holds the actual priority value.
// B[i].mpacity: this field contains the number of inactive.
//nodes in the sub-tree rooted at B[i].
class BNode(val level: Int) extends Bundle {
  val entry    = new Entry  // B[i].value
  val capacity = UInt(capacity_width(level).W)
  def <(that: BNode): Bool = (this.entry < that.entry) || !that.entry.existing
}

object BNode {
  def default(level: Int): BNode = {
    val bnode = Wire(new BNode(level))
    bnode.entry.existing := false.B
    bnode.entry.metadata := 0.U(metadata_width.W)
    bnode.entry.rank := -1.S(rank_width.W).asUInt
    bnode.capacity := ((1 << (count_of_levels - level)) - 1).U
    bnode
  }
}

//  T[i].operation: this field holds an instruction.
//  T[i].value: this field may hold a priority value that needs to be inserted into B.
//  T[i].position: this field can hold the index of a node at level.
class TNode(val level: Int) extends Bundle {
  val operation = new Operator
  val value     = new Entry // T[i].value
  val position  = UInt(level.W)
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
