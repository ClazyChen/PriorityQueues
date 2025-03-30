package fpga.sr

import chisel3._
import fpga.Const._
import fpga._

class ShiftRegister extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val blockArray = Seq.fill(count_of_entries)(Module(new Block))

    for (i <- 0 until count_of_entries) {
        blockArray(i).io.op_in := io.op_in

        if (i > 0) {
            blockArray(i).io.prev_entry_in := blockArray(i - 1).io.entry_out
            blockArray(i).io.prev_cmp_in := blockArray(i - 1).io.cmp_out
        }
        if (i < count_of_entries - 1) {
            blockArray(i).io.next_entry_in := blockArray(i + 1).io.entry_out
            blockArray(i).io.next_cmp_in := blockArray(i + 1).io.cmp_out
        }
    }
    blockArray.head.io.prev_entry_in := DontCare
    blockArray.head.io.prev_cmp_in := false.B
    blockArray.last.io.next_entry_in := Entry.default
    blockArray.last.io.next_cmp_in := true.B

//    when (blockArray.head.io.cmp_out && io.op_in.pop) {
//        io.entry_out := io.op_in.push
//    }.otherwise {
        io.entry_out := blockArray.head.io.entry_out
   // }
}