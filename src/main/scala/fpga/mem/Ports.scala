package fpga.mem

import chisel3._
import chisel3.util._

// we implement two types of memory in this repo
// 1. single port memory (1RW)
// 2. pseudo-dual-port memory (1R1W)

// The read port for pseudo-dual-port memory
class ReadPort(
    val addr_width: Int,
    val data_width: Int,
) extends Bundle {
    val en = Input(Bool())
    val addr = Input(UInt(addr_width.W))
    val data = Output(UInt(data_width.W))
}

// The write port of pseudo-dual-port memory
class WritePort(
    val addr_width: Int,
    val data_width: Int,
) extends Bundle {
    val en = Input(Bool())
    val addr = Input(UInt(addr_width.W))
    val data = Input(UInt(data_width.W))
}

// The read/write port of single port memory
class RWPort(
    val addr_width: Int,
    val data_width: Int,
) extends Bundle {
    val en = Input(Bool())
    val wen = Input(Bool())
    val addr = Input(UInt(addr_width.W))
    val data_in = Input(UInt(data_width.W))
    val data_out = Output(UInt(data_width.W))
}

// The trait of a memory
trait MemoryTrait {
    
    // read and return the entry at addr
    def read(addr: UInt): UInt
    
    // write data to the entry at addr
    def write(addr: UInt, data: UInt): Unit

    // idle the memory
    def idle(): Unit

    // for single port memory
    // idle: mem.idle()
    // read: rdata = mem.read(addr)
    // write: mem.write(addr, wdata)

    // for pseudo-dual-port memory
    // idle: mem.idle()
    // read: mem.idle(); rdata = mem.read(raddr)
    // write: mem.idle(); mem.write(waddr, wdata)
    // read & write: rdata = mem.read(raddr); mem.write(waddr, wdata)
}

// The trait of 1RW single port memory
trait SinglePortMemoryImpl extends MemoryTrait {

    // get the IO interface from the module
    def getIO: RWPort

    def read(addr: UInt): UInt = {
        val io = getIO
        io.en := true.B
        io.wen := false.B
        io.addr := addr
        io.data_out
    }

    def write(addr: UInt, data: UInt): Unit = {
        val io = getIO
        io.en := true.B
        io.wen := true.B
        io.addr := addr
        io.data_in := data
    }

    def idle(): Unit = {
        val io = getIO
        io.en := false.B
        io.wen := DontCare
        io.addr := DontCare
        io.data_in := DontCare
    }
}

// The trait of 1R1W pseudo-dual-port memory
trait DualPortMemoryImpl extends MemoryTrait {
    
    // get the IO interface from the module
    def getRPort: ReadPort
    def getWPort: WritePort

    def read(addr: UInt): UInt = {
        val rport = getRPort
        rport.en := true.B
        rport.addr := addr
        rport.data
    }

    def write(addr: UInt, data: UInt): Unit = {
        val wport = getWPort
        wport.en := true.B
        wport.addr := addr
        wport.data := data
    }

    def idle(): Unit = {
        val rport = getRPort
        val wport = getWPort
        rport.en := false.B
        wport.en := false.B
        rport.addr := DontCare
        wport.addr := DontCare
        rport.data := DontCare
    }

}

