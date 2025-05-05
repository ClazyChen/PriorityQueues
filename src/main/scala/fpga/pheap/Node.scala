package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Func._


// BNode
class Node (val level : Int) extends Bundle {
    val value = new Entry
    val capacity = UInt(capacity_width(level).W)
}
object Node {
    def default (level : Int) : Node = {
        val node = Wire(new Node(level))
        node.value := Entry.default
        val default_capacity = (1 << (count_of_levels - level + 1.U)) - 1.U
        node.capacity := (default_capacity).U(capacity_width(level).W)
        node
    }
    def generate (target_level : Int, value : Entry, capacity : Int) : Node = {
        val node = Wire(new Node(target_level))
        node.value := value
        node.capacity := capacity
        node
    }
}

// TNode
class Token (val level : Int) extends Bundle {
    val op = new Operator // equals to field operation + value(entry)
    val position = UInt(position_width(level).W)
}
object Token {
    def default (level : Int) : Token = {
        val token = Wire(new Token(level))
        token.op := Operator.nop
        token.position := 0.U(position_width(level.W)) // 0 is an invalid index
        token
    }
    def generate (target_level : Int, op : Operator, position : Int) : Token = {
        val token = Wire(new Token(target_level))
        token.op := op
        token.position := position
        token
    }
}

// pair nodes from children
class Pair (val level : Int) extends Bundle {
    val left_node = new Node(level)
    val right_node = new Node(level)
}
object Pair {
    def default (level : Int) : Pair = {
        val pair = Wire(new Pair(level))
        pair.left_node := Node.default
        pair.right_node := Node.default
        pair
    }
    def generate (target_level : Int, left : Node, right : Node) : Pair = {
        val pair = Wire(new Pair(target_level))
        pair.left_node := left
        pair.right_node := right
        pair
    }
}

// enum operation type
object State extends ChiselEnum { 
  val enq, deq, edq, nop = Value

}
