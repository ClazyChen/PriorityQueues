package fpga.sr

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class ShiftRegister extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    // the blocks in the shift register
    // broadcast the operator to all blocks
    val blocks = Seq.fill(count_of_entries)({
        val block = Module(new Block)
        block.io.op_in := io.op_in
        block
    })

    // the first block is connected to the output
    io.entry_out := blocks.head.io.entry_out

    // the dummy signal previous to the first block
    blocks.head.io.prev_entry_in := DontCare
    blocks.head.io.prev_cmp_in := false.B

    // connect the blocks one by one
    for (i <- 0 until count_of_entries - 1) {
        blocks(i) ~> blocks(i + 1)
    }

    // the dummy signal next to the last block
    blocks.last.io.next_entry_in := DontCare
    blocks.last.io.next_cmp_in := true.B
}

