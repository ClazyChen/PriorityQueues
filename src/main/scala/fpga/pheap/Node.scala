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
    def init(level: Int, entry: Entry, capacity: Int = -1): Node = {
        val cap = if (capacity == -1) get_capacity(level) else capacity
        val node = Wire(new Node(level))
        node.entry := entry
        node.capacity := cap.U
        node
    }
}

// Memory 模块的 IO 端口定义
class MemoryIO(
    val level: Int,
    val data_width: Int = rank_width
) extends Bundle {
    val r = new ReadPort(position_width(level), data_width)
    val w = new WritePort(position_width(level), data_width)
}


class Memory (
    val level: Int,
    val data_width: Int = rank_width,        
    val use_sram: Boolean = use_sram_param     
) extends Module {
    val data_depth = get_data_depth(level)
    val addr_width = position_width(level)

    val io = IO(new MemoryIO(addr_width, data_width))

    val mem = if(use_sram) Module(new Sram(data_depth, data_width))
            else Module(new FFMem(data_depth, data_width))

    mem.getRPort <> io.r
    mem.getWPort <> io.w

    def write(addr: UInt, data: UInt) = mem.write(addr, data)
    def read(addr: UInt) = mem.read(addr)
    
    // mem.getRPort.en   := io.r.en
    // mem.getRPort.addr := io.r.addr
    // mem.getWPort.en   := io.w.en
    // mem.getWPort.addr := io.w.addr
    // mem.getWPort.data := io.w.data
    // io.r.data := mem.getRPort.data

    // 在Ports.scala里面有定义
    // Encapsulated read operation.
    // 'level' could be used to tailor the returned Node (e.g., for type casting or pipelining considerations)
    // def read(addr: UInt): Node = {
    //     // Configure read port: activate read, disable write.
    //     io.r.en   := true.B
    //     io.r.addr := addr
    //     io.w.en   := false.B
    //     io.w.addr := DontCare
    //     io.w.data := DontCare

    //     // Return the data casted as Node. The 'level' parameter can be used for further customization.
    //     io.r.data.asTypeOf(new Node(level))
    // }

    // // Encapsulated write operation.
    // def write(addr: UInt, node: Node)  = {
    //     // Configure write port: activate write, disable read.
    //     io.r.en   := false.B
    //     io.r.addr := DontCare
    //     io.w.en   := true.B
    //     io.w.addr := addr
    //     io.w.data := node.asUInt
    // }
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

