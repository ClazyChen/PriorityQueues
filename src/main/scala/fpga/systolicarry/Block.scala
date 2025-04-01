package fpga.systolicarry

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._


// 定义 PriorityEntry Bundle，用于存储具有优先级和流标号的条目
class PriorityEntry(val priorityWidth: Int, val flowWidth: Int, val subWidth: Int) extends Bundle {
  // 定义条目的优先级，位宽为 priorityWidth
  val priority = UInt(flowWidth.W)
  // 定义条目的流标号，位宽为 flowWidth
  val flowId   = UInt(flowWidth.W)
  // 定义元素下标，位宽为 subwidth
  val subscript = UInt(subWidth.W)
}

// PriorityEntry 的伴生对象，提供一个 default 方法，用于生成默认（全零）条目
object PriorityEntry {
  def default(priorityWidth: Int, flowWidth: Int, subWidth: Int): PriorityEntry = {
    val flow_element = Wire(new PriorityEntry(priorityWidth, flowWidth, subWidth))
    flow_element.priority  := -1.S(priorityWidth.W).asUInt
    flow_element.flowId    := 0.U
    flow_element.subscript := -1.S(priorityWidth.W).asUInt
    flow_element
  }
}

// 定义 SystolicArrayBlock 模块，代表 systolic array 中的一个单元块
class SystolicArrayBlock(val priorityWidth: Int, val flowWidth: Int, val subWidth: Int) extends Module {
  // 定义该模块的 IO 接口
  val io = IO(new Bundle{
    val enqueue_Signal      = Input(Bool())       // 入队使能信号，当为 true 时触发入队操作
    val dequeue_Signal      = Input(Bool())       // 出队使能信号，当为 true 时触发出队操作


    val pre_entry_in = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))    // 来自右侧模块的入队条目
    val next_entry_in  = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))    // 来自左侧模块的出队条目（用于更新当前存储条目）


    val enqueue_Output_Signal = Output(Bool())    // 入队输出使能信号，用于驱动左侧模块的入队操作
    val dequeue_Output_Signal = Output(Bool())    // 出队输出使能信号，用于驱动右侧模块的出队操作
    val enqueue_Left_Entry    = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))  // 向左侧传递的入队条目
    val dequeue_Right_Entry   = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))  // 向右侧传递的出队条目

    val stored_Entry = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))  // 当前存储的条目，作为该模块的内部状态输出
  })

  // 定义寄存器 entry 用于保存当前条目，初始值为默认（全零）条目
  val entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth, subWidth))
  val next_entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth, subWidth))

  next_entry := entry
  io.stored_Entry := entry
  io.enqueue_Output_Signal := false.B
  io.dequeue_Output_Signal := false.B
  io.enqueue_Left_Entry    := entry
  io.dequeue_Right_Entry   := entry

  // 当入队信号有效时执行入队操作
  when(io.enqueue_Signal) {
    when(io.pre_entry_in.priority <= entry.priority) {
      // 若右侧传入的条目优先级大于或等于当前条目的优先级
      // 触发入队输出使能，通知左侧模块有新条目进入
      // 更新当前存储的条目为右侧传入的条目
      io.enqueue_Output_Signal := true.B
      next_entry := io.pre_entry_in
    }.elsewhen(io.pre_entry_in.priority > entry.priority) {
      // 否则（右侧条目的优先级低于当前条目的优先级）
      // 同样触发入队输出使能
      // 将右侧的条目传递给左侧模块，继续后续级联处理
      io.enqueue_Output_Signal := true.B
      io.enqueue_Left_Entry    := io.pre_entry_in
    } .otherwise {
      // 其他情况下不做操作（保持当前状态）
    }

  }.elsewhen(io.dequeue_Signal) {
    // 当出队信号有效时执行出队操作
    // 将当前存储的条目通过右侧输出传递出去
    // 更新当前条目为来自左侧的条目
    // 触发出队输出使能信号
    io.dequeue_Right_Entry   := entry
    next_entry := io.next_entry_in
    io.dequeue_Output_Signal := true.B
  }.otherwise {
    // 否则保持当前状态，不做操作
  }
  entry := next_entry
}

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