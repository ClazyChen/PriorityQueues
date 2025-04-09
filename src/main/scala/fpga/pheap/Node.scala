package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.Const._

def get_capacity(level: Int) = ((1 << (count_of_levels - level)) - 1)

def capacity_width(level: Int) = log2Ceil(get_capacity(level))

def position_width(level: Int) = Mux(level == 0, 1, level)

// TODO 下面的几个函数都需要相应修改
def get_data_depth(level: Int) = 1 << level

// 这里idx是在整个堆中的索引,pos是当前level中的索引
def idx2pos(level: Int, idx: Int) = Mux(level == 0, 0, idx(level - 1, 0))

def pos2idx(level: Int, pos: Int) = Mux(level == 0, 0, (1 << (level - 1)) + pos)

def get_lc_pos(level: Int, pos: Int): Int = {
    val parent_idx = pos2idx(level, pos)
    val lc_idx = 2 * parent_idx + 1
    idx2pos(lc_idx)
}
def get_rc_pos(level: Int, pos: Int): Int = {
    val parent_idx = pos2idx(level, pos)
    val rc_idx = 2 * parent_idx + 2
    idx2pos(rc_idx)
}


class Node(val level: Int) extends Bundle {
    val value = new Entry
    val capacity = UInt(capacity_width(level).W)
}

object Node {
    def init(level: Int, inValue: Entry): Node = {
        val node = Wire(new Node(level))
        node.value := inValue
        node.capacity := get_capacity(level).U
        node
    }
}



class Memory (
    val data_depth: Int
    val data_width: Int = rank_width,        
    val use_sram: Boolean = use_sram_param     
) extends Module {
    val addr_width = log2Ceil(data_depth)
    val io = IO(new Bundle{
        val r = new ReadPort(addr_width, data_width)
        val w = new WritePort(addr_width, data_width)
    })

    val mem = if(use_sram) Module(new Sram(data_depth, data_width))
            else Module(new FFMem(data_depth, data_width))
    
    mem.io.r.en   := io.r.en
    mem.io.r.addr := io.r.addr
    mem.io.w.en   := io.w.en
    mem.io.w.addr := io.w.addr
    mem.io.w.data := io.w.data
    io.r.data := mem.io.r.data
}

def read(memory: Memory, addr: UInt): Node = {
    memory.io.r.en   := true.B
    memory.io.r.addr := addr
    memory.io.w.en   := false.B
    memory.io.w.addr := DontCare
    memory.io.w.data := DontCare
    memory.io.data_out.asTypeOf(new Node(level))
}

def write(memory: Memory, addr: UInt, node: Node): Unit = {
    memory.io.r.en   := false.B
    memory.io.r.addr := DontCare
    memory.io.w.en   := true.B
    memory.io.w.addr := addr
    memory.io.w.data := node.asUInt
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

    def init(level: Int, entry: Entry, op: Operator): TokenNode = {
        val token_node = Wire(new TokenNode(level)) 
        token_node.entry := entry
        token_node.op := op
        token_node.position := 0.U
        token_node
    }
}

