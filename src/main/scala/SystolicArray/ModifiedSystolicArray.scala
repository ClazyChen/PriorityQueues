package SystolicArray

import chisel3._
import chisel3.util._

class ShiftRegisterBlock(priorityWidth: Int, flowWidth: Int) extends Module {
  val io = IO(new Bundle{
    val new_Entry      = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))       // 入队时，通过全局总线接收的新条目
    val enqueue_Signal = Input(Bool())                                                   // 入队操作使能信号（当 enq 有效且 deq 未激活时进入入队逻辑）
    val dequeue_Signal = Input(Bool())                                                   // 出队操作使能信号（当deq 有效时进入出队逻辑）

    val shift_Flag_In  = Input(Bool())                                                    // 入队模式下，从右侧相邻模块接收标志
    val shift_Flag_Out = Output(Bool())                                                   // 入队模式下的移位标志，若本模块发生了新元素的入队或右侧模块的已经接收了新元素，则输出为 true
    val enqueue_Right_Entry = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 入队模式下，从右侧相邻模块接收移位数据及其标志
    val enqueue_Left_Entry  = Output(Valid(new PriorityEntry(priorityWidth, flowWidth)))  // 入队模式下，将本模块需要右移的与元素通过此端口移位给左侧模块

    val dequeue_Left_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth))          // 出队模式下，从左侧相邻模块获取数据（用于右移）
    val dequeue_Right_Entry = Output(new PriorityEntry(priorityWidth, flowWidth))         // 出队模式下，此输出用于传递给右侧模块

    val stored_entry = Output(new PriorityEntry(priorityWidth, flowWidth))             // 出队模式下，此输出用于产生最终的输出结果
  })

  val entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth))

  io.stored_entry := entry
  io.shift_Flag_Out := false.B
  io.enqueue_Left_Entry.valid := false.B
  io.enqueue_Left_Entry.bits  := entry
  io.dequeue_Right_Entry      := entry


  when(io.enqueue_Signal) {
    // 入队操作：局部决策逻辑
    // 只有当来自右侧模块未发生移位且新条目有效时，
    // 若新条目的优先级高于当前条目，则本模块锁存新条目，并将原条目作为移位数据输出给左侧模块。
    when(io.new_Entry.valid && !io.shift_Flag_In && (io.new_Entry.bits.priority > entry.priority)) {
      // 锁存新条目，同时输出原条目实现向左侧模块移位
      io.enqueue_Left_Entry.valid := true.B
      io.shift_Flag_Out := true.B
      entry := io.new_Entry.bits
    } .elsewhen(io.shift_Flag_In) {
      // 如果本模块的右侧模块已锁存新条目，则本模块从右侧接收移位数据，实现条目向左移动
      io.enqueue_Left_Entry.bits  := entry
      io.enqueue_Left_Entry.valid := true.B
      io.shift_Flag_Out := true.B
      entry := io.enqueue_Right_Entry.bits
    } .otherwise {
      // 否则保持当前条目不变
    }
  } .elsewhen(io.dequeue_Signal) {
    io.stored_entry := entry
    // 出队操作：模块0的条目将被读取，而其他模块则从左侧模块接收数据，实现所有条目向右移动
    io.dequeue_Right_Entry := entry
    entry := io.dequeue_Left_Entry
  } .otherwise {
    // 否则保持当前条目不变
  }
}

class ModifiedSystolicArrayBlock(val priorityWidth: Int, val flowWidth: Int, val depth: Int) extends Module {
  //  val new_Entry      = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))       // 入队时，通过全局总线接收的新条目
  //  val enqueue_Signal = Input(Bool())                                                   // 入队操作使能信号（当 enq 有效且 deq 未激活时进入入队逻辑）
  //  val dequeue_Signal = Input(Bool())                                                   // 出队操作使能信号（当deq 有效时进入出队逻辑）
  //
  //  val shift_Flag_In  = Input(Bool())                                                    // 入队模式下，从右侧相邻模块接收标志
  //  val shift_Flag_Out = Output(Bool())                                                   // 入队模式下的移位标志，若本模块发生了新元素的入队或右侧模块的已经接收了新元素，则输出为 true
  //  val enqueue_Right_Entry = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 入队模式下，从右侧相邻模块接收移位数据及其标志
  //  val enqueue_Left_Entry  = Output(Valid(new PriorityEntry(priorityWidth, flowWidth)))  // 入队模式下，将本模块需要右移的与元素通过此端口移位给左侧模块
  //
  //  val dequeue_Left_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth))          // 出队模式下，从左侧相邻模块获取数据（用于右移）
  //  val dequeue_Right_Entry = Output(new PriorityEntry(priorityWidth, flowWidth))         // 出队模式下，此输出用于传递给右侧模块
  //
  //  val stored = Output(new PriorityEntry(priorityWidth, flowWidth))                      // 出队模式下，此输出用于产生最终的输出结果
  val io = IO(new Bundle {
    val enqueue_Signal = Input(Bool())
    val dequeue_Signal = Input(Bool())

    val block_enqueue_entry = Input(new PriorityEntry(priorityWidth, flowWidth))
    val block_dequeue_entry = Output(new PriorityEntry(priorityWidth, flowWidth))

    val data_check     = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth)))
  })

  // 实例化一组ShiftBlocks
  val shiftblocks = VecInit(Seq.fill(depth)(Module(new ShiftRegisterBlock(priorityWidth, flowWidth)).io))

  for(i <- 0 until depth) {
    io.data_check(i) := shiftblocks(i).stored_entry
  }
  // 新入队的元素显然不会插入最右侧Block的右侧
  // 最右侧的Block的右侧显然没有Block
  shiftblocks(0).shift_Flag_In := false.B
  shiftblocks(0).enqueue_Right_Entry.valid := false.B
  shiftblocks(0).enqueue_Right_Entry.bits  := PriorityEntry.default(priorityWidth, flowWidth)

  for(i <- 0 until depth) {
    shiftblocks(i).new_Entry.bits := io.block_enqueue_entry
    shiftblocks(i).new_Entry.valid := io.enqueue_Signal
    shiftblocks(i).enqueue_Signal := io.enqueue_Signal && !io.dequeue_Signal
  }
  // 模块 i+1 的移位输入连接来自模块 i 的移位输出及标志
  for(i <- 0 until depth-1) {
    shiftblocks(i+1).enqueue_Right_Entry <> shiftblocks(i).enqueue_Left_Entry
    shiftblocks(i+1).shift_Flag_In := shiftblocks(i).shift_Flag_Out
  }

  // 最左侧没有左侧邻接模块，故将其 deq_Left_Entry 置为默认值(即优先级和流标号均为0)
  shiftblocks(depth-1).dequeue_Left_Entry := PriorityEntry.default(priorityWidth, flowWidth)
  for(i <- 0 until depth) {
    shiftblocks(i).dequeue_Signal := io.dequeue_Signal
  }
  // 出队模式下，各模块之间通过左侧传递数据实现整体右移
  for(i <- 0 until depth-1) {
    shiftblocks(i).dequeue_Left_Entry := shiftblocks(i+1).dequeue_Right_Entry
  }
  // 出队输出来自模块0（始终保存最高优先级条目）
  io.block_dequeue_entry := shiftblocks(0).stored_entry
}

class ModifiedSystolicArrayPriorityQueue(val priorityWidth: Int, val flowWidth: Int, val depth: Int, val length: Int) extends Module{
  val io = IO(new Bundle {

  })
}
