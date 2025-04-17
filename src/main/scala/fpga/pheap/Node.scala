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
}


class Memory (
    val level: Int,
    val data_width: Int = rank_width,        
    val use_sram: Boolean = use_sram_param     
) extends Module {
    val data_depth = get_data_depth(level)
    val addr_width = position_width(level)

    val mem = if(use_sram) Module(new Sram(data_depth, data_width))
            else Module(new FFMem(data_depth, data_width))

    def write(addr: UInt, node: Node) = mem.write(addr, node.asUInt)
    def read(addr: UInt) = mem.read(addr).asTypeOf(new Node(level))
    
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
        token_node.entry := entry
        token_node.op := op
        token_node.position := 0.U
        token_node
    }
}

