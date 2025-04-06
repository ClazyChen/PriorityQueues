package fpga.sa

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// a block in the systolic array
class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator) // from the previous block
        val op_out = Output(new Operator) // to the next block
        val entry_in = Input(new Entry) // from the next block
        val entry_out = Output(new Entry) // to the previous block
    })

    // the entry stored in the block
    val entry = RegInit(Entry.default)

    // the operation to be performed in the next block
    val op_out = RegInit(Operator.nop)
    io.op_out := op_out

    // make comparison
    val (entry_updated, entry_next) = io.op_in.push.minmax(entry)

    // update the entry
    when (io.op_in.pop) {
        entry := io.entry_in
    } .otherwise {
        entry := entry_updated
    }
    
    // pass push/pop signals to the next block
    op_out.pop := io.op_in.pop
    op_out.push := entry_next

    // pass the updated entry to the previous block
    io.entry_out := entry_updated

    // connect blocks, this ~> next
    def ~>(next: Block) = {
        next.io.op_in := this.io.op_out
        this.io.entry_in := next.io.entry_out
    }
}
