package ShiftRegister
import chisel3._
import chisel3.util._


class PriorityEntry(val priorityWidth: Int, val flowWidth: Int) extends Bundle {
  val priority = UInt(priorityWidth.W)      // 定义元素的优先级
  val flowId   = UInt(flowWidth.W)          // 定义元素的流标号
}

object PriorityEntry {
  def default(priorityWidth: Int, flowWidth: Int): PriorityEntry = {
    val flow_element = Wire(new PriorityEntry(priorityWidth, flowWidth))
    flow_element.priority := 0.U            // 对元素进行初始化
    flow_element.flowId   := 0.U
    flow_element
  }
}

// 单个优先级队列模块
// 每个模块包含：
//  - 一个保存寄存器用于存储条目；
//  - 比较器：用于比较新条目与当前条目的优先级；
//  - 多路复用器及决策逻辑：决定是锁存新条目、还是从右侧模块移位、或保持当前条目不变。
class ShiftRegisterBlock(priorityWidth: Int, flowWidth: Int) extends Module {

  val io = IO(new Bundle{
    val new_Entry       = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 入队时，通过全局总线接收的新条目

    val enqueue_Signal  = Input(Bool())                                               // 入队操作使能信号（当 enq 有效且 deq 未激活时进入入队逻辑）
    val enq_Right_Entry = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 入队模式下，从右侧相邻模块接收移位数据及其标志
    val shift_Flag_In   = Input(Bool())                                               // 入队模式下，从右侧相邻模块接收标志
    val shift_Flag_Out  = Output(Bool())                                              // 入队模式下的移位标志，若本模块发生了新元素的入队或右侧模块的已经接收了新元素，则输出为 true
    val enq_Left_Entry  = Output(Valid(new PriorityEntry(priorityWidth, flowWidth)))  // 入队模式下，将本模块需要右移的与元素通过此端口移位给左侧模块

    val dequeue_Signal  = Input(Bool())                                               // 出队操作使能信号（当deq 有效时进入出队逻辑）
    val deq_Left_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth))          // 出队模式下，从左侧相邻模块获取数据（用于右移）
    val deq_Right_Entry = Output(new PriorityEntry(priorityWidth, flowWidth))         // 出队模式下，此输出用于传递给右侧模块
    val stored          = Output(new PriorityEntry(priorityWidth, flowWidth))         // 出队模式下，此输出用于产生最终的输出结果


  })

  val entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth))

  io.shift_Flag_Out       := false.B
  io.enq_Left_Entry.valid := false.B
  io.enq_Left_Entry.bits  := entry
  io.deq_Right_Entry      := entry
  io.stored               := entry

  when(io.enqueue_Signal) {
    // 入队操作：局部决策逻辑
    // 只有当来自右侧模块未发生移位且新条目有效时，
    // 若新条目的优先级高于当前条目，则本模块锁存新条目，并将原条目作为移位数据输出给左侧模块。
    when(io.new_Entry.valid && !io.shift_Flag_In && (io.new_Entry.bits.priority > entry.priority)) {
      // 锁存新条目，同时输出原条目实现向左侧模块移位
      io.enq_Left_Entry.valid := true.B
      io.shift_Flag_Out       := true.B
      entry := io.new_Entry.bits
    } .elsewhen(io.shift_Flag_In) {
      // 如果本模块的右侧模块已锁存新条目，则本模块从右侧接收移位数据，实现条目向左移动
      io.enq_Left_Entry.bits  := entry
      io.enq_Left_Entry.valid := true.B
      io.shift_Flag_Out       := true.B
      entry := io.enq_Right_Entry.bits
    } .otherwise {
      // 否则保持当前条目不变
    }
  } .elsewhen(io.dequeue_Signal) {
    io.stored := entry
    // 出队操作：模块0的条目将被读取，而其他模块则从左侧模块接收数据，实现所有条目向右移动
    io.deq_Right_Entry := entry
    entry := io.deq_Left_Entry
  } .otherwise {
    // 否则保持当前条目不变
  }
}
// 顶层 PriorityQueue 模块，参数化队列容量、数据宽度和优先级宽度
// 此模块实例化多个 Block，并根据 enq/deq 信号将它们按链连接起来
class PriorityQueue(val priorityWidth: Int, val flowWidth: Int, val depth: Int) extends Module {
  val io = IO(new Bundle {
    val dequeue_Signal = Input(Bool())    // 出队操作使能：当为 true 时进行出队（移位）操作
    val dequeue_Entry  = Output(new PriorityEntry(priorityWidth, flowWidth))        // 出队接口：正常情况下直接输出module(0)
    val enqueue_Entry  = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))  // 入队接口：带 valid 信号的新条目输入

    val data_check      = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth)))
  })

  // 实例化一组 Blcok
  val modules = VecInit(Seq.fill(depth)(Module(new ShiftRegisterBlock(priorityWidth, flowWidth)).io))

  for(i <- 0 until depth) {
    io.data_check(i) := modules(i).stored
  }

  // 入队模式下，各模块之间通过“移位数据”传递实现局部决策链：
  // 最右侧模块没有右侧邻接模块，故移位输入及标志置 false
  modules(0).enq_Right_Entry.valid := false.B
  modules(0).enq_Right_Entry.bits  := PriorityEntry.default(priorityWidth, flowWidth)
  modules(0).shift_Flag_In         := false.B
  // 将入队数据和使能信号广播到所有模块
  for(i <- 0 until depth) {
    modules(i).new_Entry <> io.enqueue_Entry
    // 当入队有效且当前不在出队操作中时，进入入队逻辑
    modules(i).enqueue_Signal := io.enqueue_Entry.valid && !io.dequeue_Signal
  }
  // 模块 i+1 的移位输入连接来自模块 i 的移位输出及标志
  for(i <- 0 until depth-1) {
    modules(i+1).enq_Right_Entry <> modules(i).enq_Left_Entry
    modules(i+1).shift_Flag_In   := modules(i).shift_Flag_Out
  }

  // 最左侧没有左侧邻接模块，故将其 deq_Left_Entry 置为默认值(即优先级和流标号均为0)
  modules(depth-1).deq_Left_Entry  := PriorityEntry.default(priorityWidth, flowWidth)
  // 所有模块均响应出队信号
  for(i <- 0 until depth) {
    modules(i).dequeue_Signal := io.dequeue_Signal
  }
  // 出队输出来自模块0（始终保存最高优先级条目）
  io.dequeue_Entry := modules(0).stored
  // 出队模式下，各模块之间通过左侧传递数据实现整体右移
  for(i <- 0 until depth-1) {
    modules(i).deq_Left_Entry := modules(i+1).deq_Right_Entry
  }

}
