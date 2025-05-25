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
        node.capacity := -1.S(capacity_width(level).W).asUInt
        node
    }
    def getWidth (level : Int) : Int = (new Node(level)).getWidth
}

// TNode
class Token (val level : Int) extends Bundle {
    val op = new Operator // equals to operation + value(entry)
    val position = UInt(position_width(level).W)
}
object Token {
    def default (level : Int) : Token = {
        val token = Wire(new Token(level))
        token.op := Operator.nop
        token.position := DontCare
        token
    }
    def getWidth (level : Int) : Int = (new Token(level)).getWidth
}

// pair nodes from children
class Pair (val level : Int) extends Bundle {
    val left_node = new Node(level)
    val right_node = new Node(level)
}
object Pair {
    def default (level : Int) : Pair = {
        val pair = Wire(new Pair(level))
        pair.left_node := Node.default(level)
        pair.right_node := Node.default(level)
        pair
    }
    def getWidth (level : Int) : Int = (new Pair(level)).getWidth
}
