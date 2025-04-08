package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.Const._
import fpga.pheap.Param.Const._

def capacity_width(level: Int): Int = {
    // 只有1个节点时，level = 1
    log2Ceil((1 << (pheap_levels + 1 - level)) - 1)
}


class Node(val level: Int) extends Bundle {
    val value = new Entry
    val capacity = UInt(capacity_width(level).W)
}

object Node {
    def init(level: Int, inValue: Entry): Node = {
        val node = Wire(new Node(level))
        node.value := inValue
        node.capacity := ((1 << (pheap_levels + 1 - level)) - 1).U

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

    def readNode(addr: UInt): Node = {
        memory.io.r.addr := addr
        memory.io.r.en   := true.B
        memory.io.data_out.asTypeOf(new Node(level))
    }

    def writeNode(addr: UInt, node: Node): Unit = {
        memory.io.w.addr := addr
        memory.io.w.en   := true.B
        memory.io.w.data := node.asUInt
    }

    val localAddr = token_in.position

    when (!io.token_in.op.pop) {
        val tokenPos = token.position
        val tokenEntry = token.entry

        val storedNode = readNode(localAddr)

        when (!storedNode.value.existing) {
            val newNode = Wire(Node.init(level, tokenEntry))
            writeNode(localAddr, newNode)
        } .otherwise (storedNode.value < tokenEntry) {
            val newNode = Wire(Node.init(level, tokenEntry))
            writeNode(localAddr, newNode)
            token.entry := storedNode.value
        }

        
        when ()
        
    } .otherwise {

    }
}
