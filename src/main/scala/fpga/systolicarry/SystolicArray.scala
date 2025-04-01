package fpga.systolicarry

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// 定义顶层 SystolicArray 模块，由多个 SystolicArrayBlock 级联组成
class SystolicArray(val priorityWidth: Int, val flowWidth: Int, val depth: Int, val subWidth: Int) extends Module {
  // 定义顶层模块的 IO 接口
  val io = IO(new Bundle {
    val enqueue_Signal = Input(Bool())  // 入队使能信号（外部输入）
    val dequeue_Signal = Input(Bool())  // 出队使能信号（外部输入）
    val enqueue_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))  // 入队条目（外部输入）
    val dequeue_Entry  = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth)) // 出队条目（外部输出），输出来自某个级联模块的当前条目

    val data_check     = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth, subWidth)))  // 调试输出，显示整个 systolic array 中每个模块当前存储的条目
  })

  // 实例化一个深度为 depth 的 SystolicArrayBlock 向量，每个块的 IO 被保存在 modules 中
  val modules = VecInit(Seq.fill(depth)(Module(new SystolicArrayBlock(priorityWidth, flowWidth, subWidth)).io))

  // 将各模块当前存储的条目通过 data_check 输出，便于调试观察
  for(i <- 0 until depth) {
    io.data_check(i) := modules(i).stored_Entry
  }

  // 为第 0 个模块连接外部信号：
  // - 入队使能信号直接来自顶层
  // - 出队使能信号直接来自顶层
  // - 入队数据直接来自外部输入
  modules(0).enqueue_Signal := io.enqueue_Signal
  modules(0).dequeue_Signal := io.dequeue_Signal
  modules(0).pre_entry_in := io.enqueue_Entry
  modules(0).next_entry_in  := modules(1).dequeue_Right_Entry

  // 对最后一个模块的 dequeue_Left_Entry 赋予默认值，确保所有输入均被初始化
  modules(depth-1).pre_entry_in := PriorityEntry.default(priorityWidth, flowWidth, subWidth)

  // 将顶层的出队条目连接到第 0 个模块的存储条目
  io.dequeue_Entry := modules(0).stored_Entry

  // 将各模块通过信号级联连接起来，形成一个流水传输结构
  for(i <- 1 until depth) {
    // 当前模块的入队使能信号来源于前一模块的入队输出使能
    // 当前模块的出队使能信号来源于前一模块的出队输出使能
    // 当前模块的入队数据来源于前一模块向左传递的条目
    // 前一模块的出队输入数据由当前模块向右传递的条目提供
    modules(i).enqueue_Signal := modules(i-1).enqueue_Output_Signal
    modules(i).dequeue_Signal := modules(i-1).dequeue_Output_Signal
    modules(i).pre_entry_in  := modules(i-1).enqueue_Left_Entry
    modules(i-1).next_entry_in := modules(i).dequeue_Right_Entry
  }
}