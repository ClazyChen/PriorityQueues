package fpga.systolicarray

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// 定义脉动阵列Block结构
class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)         // 每个块从外部/其他块接收的信号，可以是一个新的entry或者pop信号
        val entry_out = Output(new Entry)       // 对其他block输出的entry
        val prev_entry_in = Input(new Entry)    // 从前一个block接收的entry
        val next_entry_in = Input(new Entry)    // 从后一个block接收的entry
        val prev_cmp_in = Input(Bool())         // 从前一个块接收的比较结果
        val next_cmp_in = Input(Bool())         // 从后一个块接收的比较结果
        val cmp_out = Output(Bool())            //当前block向其他block输出的比较结果
    })
    // 初始化两个寄存器
    val entry_holder = RegInit(Entry.default)
    val tmp_holder = RegInit(Entry.default)
}