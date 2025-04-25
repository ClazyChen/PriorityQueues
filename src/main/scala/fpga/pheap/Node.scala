package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// 用entry数量来计算level的数量
// 把count_of_entries转化成count_of_levels
def generate_level (current_node_index : Int) : Int = {
    log2Ceil(current_node_index + 1.U)
}
// 计算capacity位宽
def capacity_width (cur_level : Int) : Int = {
    count_of_levels - cur_level + 1.U
}
// 计算position位宽
def position_width (cur_level : Int) : Int = {
    cur_level
}
// BNode
class Node (val level : Int) extends Bundle {
    val value = new Entry
    val capacity = UInt(capacity_width(level).W)
}
object Node {
    def default (level) : Node = {
        val node = Wire(new Node(level))
        node.value := Entry.default
        // 计算node的默认capacity值，用于初始化
        // Q ：同一层的结点可能有不同的capacity，这里的default_capacity没有解决这个问题
        val default_capacity = (1 << count_of_levels - level) + 1.U) - 1.U
        node.capacity := (default_capacity).U(capacity_width(level).W)
    }
}
// TNode
class Token (val level : Int) extends Bundle {
    val op = new Operator // equals to field operation + value
    val position = UInt(position_width(level).W)
}
object Token {
    def default (level : Int) : Token = {
        val token = Wire(new Token(level))
        token.op := Operator.nop
        token.position := 0.U(position_width(level.W))
    }
}
// 枚举操作类型
object State extends ChiselEnum { 
  val enq, deq, edq, nop = Value
}
// 定义一些操作Pheap的辅助函数
object PheapFunc {
    // 计算块内索引
    def cal_local_index (g_index : Int,cur_level : Int) : Int = {
        g_index - (1 << (cur_level - 1)) + 1.U
    }
    //计算全局索引
    def cal_global_index (l_index : Int,cur_level : Int) : Int = {
        val upper_sum = 1.U << (cur_level - 1) - 1.U
        upper_sum + l_index
    }
    // 左孩子全局索引
    def get_lc_g_index (cur_index : Int) : Int = {
        cur_index << 1
    }
    // 右孩子全局索引
    def get_rc_g_index (cur_index : Int) : Int = {
        (cur_index << 1) + 1
    }
}
