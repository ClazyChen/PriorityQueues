package fpga.systolicarray

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// 定义脉动阵列Block结构
class Block extends Module {
    val io = IO(new Bundle {
       val op_in = Input(new Operator) // 当前块接收前一个块的输入/系统外部输入
       val op_out = Output(new Operator) // 当前块向下一个块传递的operator
       val next_entry_in = Input(new Entry) // 当前块接收到下一个块的输入entry
       val entry_out = Output(new Entry) // 对外输出的entry
    })

    // 初始化两个寄存器
    val entry_holder = RegInit(Entry.default)
    val tmp_holder = RegInit(Entry.default)

    // 初始化输出端口
    io.op_out.push := Entry.default
    io.op_out.pop := false.B
    io.entry_out := entry_holder

    // 用变量保存rank比较状态
    val cmp_status = io.op_in.push < entry_holder

    // decision logic
    when (io.op_in.pop && !io.op_in.push.existing) { // 对应pop操作 
        io.op_out.push := Entry.default
        io.op_out.pop := true.B
        entry_holder := io.next_entry_in
    }
    .elsewhen (io.op_in.push.existing && !io.op_in.pop) { // 对应push操作
        when (!cmp_status) {
            tmp_holder := io.op_in.push
            io.op_out.push := tmp_holder
            io.op_out.pop := false.B
        }
        .otherwise { // cmp_status
            tmp_holder := entry_holder
            entry_holder := io.op_in.push
            io.op_out.push := tmp_holder
            io.op_out.pop := false.B
        }
    }
    .otherwise {
        // do nothing
    }

}