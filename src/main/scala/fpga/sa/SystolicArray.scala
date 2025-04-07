package fpga.sa

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// top-module
class SystolicArray extends Module with PriorityQueueTrait {

    val io = IO(new PQIO)

    val blocks = Seq.fill(count_of_entries)(Module(new Block))

    for (i <- 0 until (count_of_entries - 1)) {
        blocks(i) ~> blocks(i + 1)
    }

    blocks.last.io.entry_in := Entry.default

    // link to the upper module
    io.entry_out := blocks.head.io.entry_out
    blocks.head.io.op_in := io.op_in
    
}
