package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.Const._
import fpga.pheap.Const._
import fpga._


class Node(val level : Int) extends Bundle {
    val entry = new Entry
    val capacity = UInt(capacity_width(level).W) 
}

// the default node (empty node)
object Node {
    def default(level : Int): Node = {
        val node       = Wire(new Node(level))
        node.entry    := Entry.default
        node.capacity := -1.S(capacity_width(level).W).asUInt // 满树初始就是全1
        node
    }

    def getWidth(level : Int): Int = (new Node(level)).getWidth
}

// the parameter of operation in this level
class Token(val level : Int) extends Bundle {
    // 与论文中的operation、value等价
    val op       = new Operator
    // the index of the node to be operated on in this level
    val position = UInt(position_width(level).W)
}

// the default token (operation nop)
object Token {
    def default(level: Int): Token = {
        val token       = Wire(new Token(level))
        token.op       := Operator.nop
        token.position := DontCare
        token
    }
}