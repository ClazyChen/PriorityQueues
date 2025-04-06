package fpga.sa

import chisel3._
import fpga.Const._
import fpga._

class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val cmp_in = Input(Bool())
        val next_entry_in = Input(new Entry)
        val op_out = Output(new Operator)
        val entry_out = Output(new Entry)
    })

    val entry = RegInit(Entry.default)
    val op = RegInit(Operator.nop)
    val cmp = io.op_in.push < entry

    val entry_hold := Mux(cmp, io.op.push, entry)
    val entry_next := Mux(cmp, entry, op.push)

    entry := entry_hold
    op.push := entry_next
    op.pop := io.op_in.pop

    io.op_out := op
    io.entry_out := entry_hold

    when(io.op_in.pop) {
        // 除了第一个block，其他block的new entry一定>= entry
        // 所以不用比较new entry和entry，然后对第一个block特殊处理一下就好
        when(cmp) {
            // replace case 1，那么后面就不用进行replace操作了
            entry := io.op_in.push
            op := Operator.default
        }.otherwise {
            // pop or replace case 2
            // 保持原参数继续向后传递
            entry := io.next_entry_in
            op := io.op_in
        }
    }.otherwise {
        // 这里是本entry和new entry的比较结果
        when(cmp_in) {
            // push case 1
            entry := io.op_in.push
            op.push := entry
        }.otherwise {
            // push case 2 or nop
            op.push := io.op_in.push
        }
        op.pop := io.op_in.pop
    }
}