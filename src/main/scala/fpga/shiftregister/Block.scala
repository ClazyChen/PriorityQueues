package fpga.shiftregister

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class Block extends Module{
  val io = IO(new Bundle {
    val op_in = Input(new Operator)
    val prev_entry_in = Input(new Entry)    // 入队操作时接收前一个模块传入的结果
    val next_entry_in = Input(new Entry)    // 出队操作时接收后一个模块传入的结果
    val prev_cmp_in   = Input(Bool())       // 接收前一个的比较结果
    val next_cmp_in   = Input(Bool())

    val cmp_out   = Output(Bool())
    val entry_out = Output(new Entry)
  })

  val entry = RegInit(Entry.default)
  io.entry_out := entry

  val cmp = io.op_in.entry_in < entry
  io.cmp_out := cmp

  when(io.op_in.push){
    // 执行入队信号
    // 情况1: 入队的元素的优先级数值大于当前元素的优先级 -> 无事发生

    // 情况2: 入队的元素的优先级数值小于当前元素的优先级 -> 观察入队的元素是否被之前的元素所接收
    when(cmp){
      // 情况2.1: 如果已经被接收（pre_cmp_in = true）-> 直接接收之前模块传来的元素
      // 情况2.2: 如果未被接收 （pre_cmp_in = false）-> 则本模块进行接收
      entry := Mux(io.prev_cmp_in, io.prev_entry_in, io.op_in.entry_in)
    }
  }.elsewhen(io.op_in.pop){
    // 执行出队信号
    entry := Mux(io.next_cmp_in, io.next_entry_in, entry)
  }.elsewhen(io.op_in.replace){
    // 执行替换操作
  }

  def ~>(next: Block) = {
    next.io.prev_entry_in := this.io.entry_out
    next.io.prev_cmp_in   := this.io.cmp_out
    this.io.next_entry_in := next.io.entry_out
    this.io.next_cmp_in   := next.io.cmp_out
  }
}
