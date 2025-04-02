package fpga.mem

import chisel3._
import chisel3.util._

// a single port memory with flip-flop
// warning: read in FFMem does not have a delay
class SinglePortFFMem(
    val data_depth: Int,
    val data_width: Int,
) extends Module with SinglePortMemoryImpl {
    val addr_width = log2Ceil(data_depth)

    val io = IO(new RWPort(
        addr_width = addr_width,
        data_width = data_width,
    ))

    val mem = Reg(Vec(data_depth, UInt(data_width.W)))
    io.data_out := DontCare

    when (io.en) {
        when (io.wen) {
            mem(io.addr) := io.data_in
        }.otherwise {
            io.data_out := mem(io.addr)
        }
    }

    // provide the IO interface
    def getIO: RWPort = io
}
