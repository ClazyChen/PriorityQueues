package fpga.sa

import chisel3._
import fpga.Const._
import fpga._

class SystolicArray extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val blockArray = Seq.fill(count_of_entries)(Module(new Block))

    val cmp = Wire(Bool())
    cmp := io.op_in.push < blockArray.head.io.entry_out

    for (i <- 0 until count_of_entries - 1) {
        blockArray(i + 1).io.op_in := blockArray(i).io.op_out
        blockArray(i + 1).io.cmp_in := blockArray(i).io.cmp_out
        blockArray(i).io.next_entry_in := blockArray(i + 1).io.entry_out
    }

    // 关键路径在哪里
    when(cmp && io.op_in.push.existing && io.op_in.pop) {
        io.entry_out := io.op_in.push
        blockArray.head.io.op_in.push := Entry.default
        blockArray.head.io.op_in.pop := false.B
    }.otherwise {
        io.entry_out := blockArray.head.io.entry_out
        blockArray.head.io.op_in := io.op_in
    }

    blockArray.head.io.cmp_in := cmp
    blockArray.last.io.next_entry_in := Entry.default
}