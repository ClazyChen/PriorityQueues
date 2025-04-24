package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.mem._
import fpga.Const._
import fpga.pheap.Param._


class Node(val level: Int) extends Bundle {
    val entry = new Entry
    val capacity = UInt(capacity_width(level).W) // 不同层的Node大小不一样
}

object Node {
    def init(level: Int, entry: Entry, capacity: UInt): Node = {
        val node = Wire(new Node(level))
        node.entry := entry
        node.capacity := capacity
        node
    }

    def default(level : Int): Node = {
        val node       = Wire(new Node(level))
        node.entry    := Entry.default
        node.capacity := -1.S(capacity_width(level).W).asUInt // 满树初始就是全1
        node
    }

    def getWidth(level : Int): Int = (new Node(level)).getWidth

}

class Pair(val level: Int) extends Bundle {
    val first  = new Node(level)
    val second = new Node(level)
}

object Pair {
    def default(level: Int): Pair = {
        val pair = Wire(new Pair(level))
        pair.first  := Node.default(level)
        pair.second := Node.default(level)
        pair
    }
}


class TokenNode(val level: Int) extends Bundle {
    val entry    = new Entry
    val op       = new Operator
    val position = UInt(position_width(level).W)
}

object TokenNode {
    def default(level: Int): TokenNode = {
        val token_node = Wire(new TokenNode(level))
        token_node.entry := Entry.default
        token_node.op := Operator.nop
        token_node.position := 0.U
        token_node
    }

    def init(level: Int, entry: Entry = Entry.default, op: Operator = Operator.nop): TokenNode = {
        val token_node = Wire(new TokenNode(level)) 
        // TODO op之中已经包含Entry了，所以这里可以不用再赋值
        token_node.entry := entry
        token_node.op := op
        token_node.position := 0.U
        token_node
    }
}

