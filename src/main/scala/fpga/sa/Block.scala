package fpga.sa

import chisel3._
import fpga.Const._
import fpga._

class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val next_entry_in = Input(new Entry)
        val op_out = Output(new Operator)
        val entry_out = Output(new Entry)
        val cmp_out = Output(Bool())
    })

    val entry = RegInit(Entry.default)

    // TODO 写法改进
    val init = Wire(new Operator)
    init.push := Entry.default
    init.pop := false.B
    val op = RegInit(init) // op里的push就相当于tmp entry

    // 相当于K为1，分支数为1的寄存器版clubheap，
    //
    // 新元素需和本元素以及下层传上来的元素比较
    val cmp1 = Wire(Bool())
    val cmp2 = Wire(Bool())

    cmp1 := io.op_in.push < entry
    cmp2 := io.op_in.push < io.next_entry_in

    io.cmp_out := cmp1
    io.op_out := op
    io.entry_out := entry

    when(io.op_in.pop) {
        // 除了第一个block，其他block的new entry一定>= entry
        // 所以不用比较new entry和entry，然后对第一个block特殊处理一下就好
        when(cmp2) {
            // replace case 1，那么后面就不用进行replace操作了
            entry := io.op_in.push
            op.push := Entry.default
            op.pop := false.B
        }.otherwise {
            // pop or replace case 2
            // 保持原参数继续向后传递
            entry := io.next_entry_in
            op := io.op_in
        }
    }.otherwise {
        // 这里是本entry和new entry的比较结果
        when(cmp1) {
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