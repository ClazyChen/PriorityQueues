package fpga.sa

import chisel3._
import fpga.Const._
import fpga._


class SystolicArray extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val blockArray = Seq.fill(count_of_entries)(Module(new Block))

    // 记录上一个操作
    val last_op = Reg(new Operator)
    last_op := io.op_in

    val op = Wire(new Operator)

    // 支持push-push，pop/replace-push，不支持push-pop/replace、pop/replace-pop/replace
    // 若当前操作为pop/replace，那上一个操作只能为nop，否则置当前操作为nop
    when (io.op_in.pop && (last_op.pop || last_op.push.existing)) {
        op.push := Entry.default
        op.pop := false.B
    }.otherwise {
        op := io.op_in
    }

    for (i <- 0 until count_of_entries - 1) {
        blockArray(i + 1).io.op_in := blockArray(i).io.op_out
        blockArray(i).io.next_entry_in := blockArray(i + 1).io.entry_out
    }

    // 对replace特殊处理一下
    // 关键路径，有待优化
    // 如果用第一个block的cmp结果会循环
    when(op.pop && op.push < blockArray.head.io.entry_out) {
        io.entry_out := op.push
        blockArray.head.io.op_in.push := Entry.default
        blockArray.head.io.op_in.pop := false.B
    }.otherwise {
        io.entry_out := blockArray.head.io.entry_out
        blockArray.head.io.op_in := op
    }
    blockArray.last.io.next_entry_in := Entry.default
}