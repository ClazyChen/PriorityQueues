package fpga.shiftregister

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// ShiftRegister Block Unit

class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)         // 每个block接收的operator
        val prev_entry_in = Input(new Entry)    // 从前一个block接收的entry
        val next_entry_in = Input(new Entry)    // 从后一个block接收的entry
        val entry_out = Output(new Entry)       // 当前block向其他block输出的entry
        val prev_cmp_in = Input(Bool())         // 当前block接收的前一个块的比较结果
        val next_cmp_in = Input(Bool())         // 当前block接收的后一个块的比较结果
        val cmp_out = Output(Bool())            // 当前block向其他block输出的entry.rank比较结果
    })

    // 用寄存器来保存当前block内部的entry
    val entry_holder = RegInit(Entry.default)

    // 当前block向其他block输出的entry
    io.entry_out := entry_holder

    // 声明一个表示比较结果的变量，用于生成cmp信号
    val cmp_status = io.op_in.push < entry_holder;
    
    // 生成cmp信号
    io.cmp_out := cmp_status

    // decision logic
    // 当io.op_in.pop使能时，有可能io.op_in.push也使能，这时起到replace的作用
    // replace的语义为：push-pop，先push一个新元素再pop出去，此时有可能新入队的元素立即被pop出去

    when (io.op_in.pop) { // 进行pop处理，同时考虑replace的情况
        when(!cmp_status) {
            entry_holder := Mux(io.next_cmp_in,io.op_in.push,io.next_entry_in) // 手动模拟得出的结论,当只有pop信号时，所有block的cmp_status都为false
        }
        .otherwise {
            // do nothing
        }
    }
    .elsewhen(io.op_in.push.existing) { // push
        when(cmp_status) {
            entry_holder := Mux(io.prev_cmp_in,io.prev_entry_in,io.op_in.push)
        }
        .otherwise {
            // do nothing
        }
    }
    .otherwise {
        // do nothing
    }
}