package fpga.sa

import chisel3._
import fpga.Const._
import fpga._

// TODO 优化比较器个数
// enqueue 可以连续 其他不行
class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val next_entry_in = Input(new Entry)
        val cmp_in = Input(Bool())
        val op_out = Output(new Operator)
        val entry_out = Output(new Entry)
        val cmp_out = Output(Bool())
    })

    val entry = RegInit(Entry.default)
    val tmp_op = RegInit(0.U.asTypeOf(new Operator)) // tmp_op里的push就相当于tmp entry
    val cmp = Wire(Bool())

    // next entry和new entry比较，用于replace；这里的new entry指的是op_in里的push
    // 还可以将结果传给next block，用于enqueue；以达到节省一个比较器的效果
    cmp := io.op_in.push < io.next_entry_in
    io.cmp_out := cmp

    // 暂存操作状态
    io.op_out := tmp_op

    io.entry_out := entry

//    when(io.op_in.push.existing && !io.op_in.pop) {
//        // 这里是本entry和new entry的比较结果
//        when (io.cmp_in) {
//            entry := io.op_in.push
//            tmp_op.push := entry
//        }.otherwise {
//            tmp_op.push := io.op_in.push
//        }
//
//        tmp_op.pop := io.op_in.pop
//    }.elsewhen(!io.op_in.push.existing && io.op_in.pop) {
//        entry := io.next_entry_in
//        tmp_op.push := io.op_in.push
//        tmp_op.pop := io.op_in.pop
//    }.elsewhen(io.op_in.push.existing && io.op_in.pop) {
//        // 除了第一个block，其他block的new entry一定>= entry
//        // 所以不用比较new entry和entry，然后对第一个block特殊处理一下就好
//        when (cmp) {
//            // 若new entry更小，那么后面就不用进行replace操作了
//            entry := io.op_in.push
//            tmp_op.push.existing := false.B
//            tmp_op.pop := false.B
//        } .otherwise {
//            // 若next entry更小，则将new entry放在tmp中继续向后传递
//            // 当next entry == new entry时可以保证new entry在后面
//            entry := io.next_entry_in
//            tmp_op.push := io.op_in.push
//        }
//    }

    when(io.op_in.pop) {
        // 除了第一个block，其他block的new entry一定>= entry
        // 所以不用比较new entry和entry，然后对第一个block特殊处理一下就好
        when(cmp) {
            // 若new entry更小，那么后面就不用进行replace操作了
            entry := io.op_in.push
            tmp_op.push.existing := false.B
            tmp_op.pop := false.B
        }.otherwise {
            // 当pop或replace时next entry更小，则赋值为next_entry_in，
            // 并保持原参数继续向后传递
            entry := io.next_entry_in
            tmp_op := io.op_in
        }
    }.otherwise {
        // 这里是本entry和new entry的比较结果
        when(io.cmp_in) {
            entry := io.op_in.push
            tmp_op.push := entry
        }.otherwise {
            tmp_op.push := io.op_in.push
        }
        tmp_op.pop := io.op_in.pop
    }
}