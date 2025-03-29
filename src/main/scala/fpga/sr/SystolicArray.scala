package fpga.sr

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class SystolicArray extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val blocks = Seq.fill(count_of_entries)({
        val block = Module(new SystolicBlock)
        block
    })

    io.entry_out := blocks.head.io.entry_out

    blocks.head.io.op_in := io.op_in
    
    for(i <- 0 until count_of_entries - 1) {
        blocks(i) -> blocks(i + 1)
    }
    blocks.last.io.next_entry_in := Entry.default

    if(debug) {
        io.dbgPort.foreach { dbgPort =>
            dbgPort := blocks.map(_.io.entry_out)
        }
        io.dbgPort1.foreach { dbgPort =>
            dbgPort := blocks.map(_.io.temp_out)
        }

    }

}