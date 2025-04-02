package fpga.mem

import chisel3._
import chisel3.util._

// a pseudo-dual-port memory with flip-flop
class FFMem(
    val data_depth: Int,
    val data_width: Int,
) extends Module with DualPortMemoryImpl {
    val addr_width = log2Ceil(data_depth)

    val io = IO(new Bundle{
        val r = new ReadPort(addr_width, data_width)
        val w = new WritePort(addr_width, data_width)
    })

    val mem = Reg(Vec(data_depth, UInt(data_width.W)))
    io.r.data := DontCare

    // notice that the read has no delay
    when (io.r.en) {
        io.r.data := mem(io.r.addr)
    }

    when (io.w.en) {
        mem(io.w.addr) := io.w.data
    }

    // handle the read and write conflict
    when (io.r.en && io.w.en && io.r.addr === io.w.addr) {
        io.r.data := io.w.data
    }

    // provide the IO interface
    def getRPort: ReadPort = io.r
    def getWPort: WritePort = io.w
    
}
