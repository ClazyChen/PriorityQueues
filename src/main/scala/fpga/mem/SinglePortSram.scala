package fpga.mem

import chisel3._
import chisel3.util._

// a single port memory with SRAM
class SinglePortSram(
    val data_depth: Int,
    val data_width: Int,
) extends Module with SinglePortMemoryImpl {
    val addr_width = log2Ceil(data_depth)

    val io = IO(new RWPort(
        addr_width = addr_width,
        data_width = data_width,
    ))

    val mem = SyncReadMem(data_depth, UInt(data_width.W))

    io.data_out := DontCare

    // data_out是wire类型，en有效的时候，data_out同时有效，为啥会延迟一个周期？
    when (io.en) {
        val port = mem(io.addr)
        when (io.wen) {
            port := io.data_in
        }.otherwise {
            io.data_out := port
        }
    }

    // provide the IO interface
    def getIO: RWPort = io
}
