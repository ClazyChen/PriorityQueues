package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.Const._
import fpga._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselStage


// TODO：这两函数不知道放哪里比较正规
def capacity_width(val height : Int) Int {
    1 << height
}

def position_width(val level: Int) Int {
    1 << level
}

// 感觉这里用height比level更好一点，因为level是从上往下算的，而capacity取决于height
// 但是用height，形式上不统一，有待抉择
class Node(val height : Int) extends Bundle {
    val entry = new Entry // whether the entry is valid
    val capacity = UInt(capacity_width(height).W) // metadata
}

// the default node (empty node)
object Node {
    def default: node = {
        val node = Wire(new Entry)
        node.entry := Entry.default
        // 直接0.U会0扩展也没问题，这里为了易读；但也可能是显而易见，不必要；
        node.capacity := 0.U(capacity_width(height).W)
        node
    }
}

// the parameter of operation in this level
class Token(val level : Int) extends Bundle {
    // 与论文中的operation、value等价
    val op = new Opeator
    // the index of the node to be operated on in this level
    val position = UInt(position_width(level).W)
}

// the default token (operation nop)
object Token {
    def default: Token = {
        val token = Wire(new Token)
        token.op := Operator.nop
        token.position := 0.U(position_width(level).W) // TODO 待考虑，目前认为0起码不会越界
        token
    }
}

object State extends ChiselEnum {
    val nop, read, exec, write = Value
}