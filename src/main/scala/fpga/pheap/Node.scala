package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.Const._
import fpga.pheap.Param._

def capacity_width(level: Int): Int = {
    // 只有1个节点时，level = 1
    log2Ceil((1 << (count_of_levels + 1 - level)) - 1)
}

def idx2pos(level: Int, idx: Int) = Mux(level <= 1, 0, idx(level - 2, 0))

def get_parent_idx(level: Int, pos: Int) = Mux(level <= 1, pos, (1 << (level - 2)) + pos)

def get_lc_pos(level: Int, pos: Int): Int = {
    val parent_idx = get_parent_idx(level, pos)
    val lc_idx = 2 * parent_idx
    idx2pos(lc_idx)
}
def get_rc_pos(level: Int, pos: Int): Int = {
    val parent_idx = get_parent_idx(level, pos)
    val rc_idx = 2 * parent_idx + 1
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
        node.capacity := ((1 << (count_of_levels + 1 - level)) - 1).U
        node
    }
}


class Memory(val data_depth: Int, val data_width: Int, val useSram: Boolean) extends Module {
    val addr_width = log2Ceil(data_depth)
    val io = IO(new Bundle{
        val r = new ReadPort(addr_width, data_width)
        val w = new WritePort(addr_width, data_width)
    })

    val mem = if(useSram) Module(new Sram(data_depth, data_width))
            else Module(new FFMem(data_depth, data_width))

    
    mem.io.r.addr := io.addr
    mem.io.r.en   := io.en
    mem.io.w.addr := io.addr
    mem.io.w.en   := io.wen
    mem.io.w.data := io.data_in
    io.data_out   := mem.io.r.data
}

def read(memory: Memory, addr: UInt): Node = {
    memory.io.r.addr := addr
    memory.io.r.en   := true.B
    memory.io.data_out.asTypeOf(new Node(level))
}

def write(memory: Memory, addr: UInt, node: Node): Unit = {
    memory.io.w.addr := addr
    memory.io.w.en   := true.B
    memory.io.w.data := node.asUInt
}

class TokenNode extends Bundle {
    val entry    = new Entry
    val op       = new Operator
    val position = UInt(level.W)
}

object TokenNode {
    def default: TokenNode = {
        val token_node = Wire(new TokenNode)
        token_node.entry := Entry.default
        token_node.op := Operator.nop
        token_node.position := 0.U
        token_node
    }

    def init(entry: Entry, op: Operator): TokenNode = {
        val token_node = Wire(new TokenNode)
        token_node.entry := entry
        token_node.op := op
        token_node.position := 0.U
        token_node
    }
}


class PHeapLevel(val level: Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new TokenNode)
        val token_out = Output(new TokenNode)
        val mem_in = Input(new Memory())
        val mem_out = Output(new Memory())
    })
    val memory = Module(new Memory(data_depth, rank_width, useSram = true))
    val token = RegInit(TokenNode.default)

    def ~>(next: PHeapLevel) = {
        next.io.token_in := this.io.token_out
        this.io.mem_in := next.io.mem_out
    }

    token := token_in
    token_out := token
    mem_out := memory

    val addr = token_in.position

    when (!io.token_in.op.pop) {
        val stored_node = read(memory, addr)

        when (!stored_node.value.existing) {
            val newNode = read(memory, addr)
            newNode.capacity -= 1
            write(memory, addr, newNode)
            token.op = Operator.nop
        } .elsewise (stored_node.value < token.entry) {
            val newNode = Wire(Node.init(level, token.entry))
            write(memory, addr, newNode)
            token.entry := stored_node.value
        } .otherwise { }

        val lc_pos = get_lc_pos(level, token.position)
        val rc_pos = lc_pos + 1
        val lc_node = read(mem_in, lc_pos)
        token.position := Mux(lc_node.capacity > 0, lc_pos, rc_pos)
        
    } .otherwise {

    }
}
