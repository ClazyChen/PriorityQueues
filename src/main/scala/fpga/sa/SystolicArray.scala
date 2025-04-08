package fpga.sa

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class SystolicArray extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    // the blocks in the systolic array
    val blocks = Seq.fill(count_of_entries)(Module(new Block))

    // the dummy signal previous to the first block
    io.entry_out := blocks.head.io.entry_out
    blocks.head.io.op_in := io.op_in

    // connect blocks one by one
    for (i <- 0 until count_of_entries - 1) {
        blocks(i) ~> blocks(i + 1)
    }

    // the dummy signal next to the last block
    blocks.last.io.entry_in := Entry.default
}
