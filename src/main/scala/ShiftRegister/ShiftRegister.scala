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
    flow_element.priority := 0.U
    flow_element.flowId   := 0.U
    flow_element
  }
}

class ShiftRegisterBlock(priorityWidth: Int, flowWidth: Int, depth: Int) extends Module {
  val io = IO(new Bundle{
    val new_Entry       = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 入队时，通过全局总线接收的新条目
    val enqueue_Signal  = Input(Bool())                                               // 入队操作使能（当 enq 有效且 deq 未激活时进入入队逻辑）
    val dequeue_Signal  = Input(Bool())                                               // 出队操作使能信号
    val enq_Right_Entry = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 入队模式下，从右侧相邻模块接收移位数据及其标志
    val deq_Left_Entry  = Input(Valid(new PriorityEntry(priorityWidth, flowWidth)))   // 出队模式下，从左侧相邻模块获取数据（用于右移）

    val shift_Flag_In   = Input(Bool())                                               // 入队模式下，从右侧相邻模块接收标志
    val shift_Flag_Out  = Output(Bool())                                              // 入队模式下的移位标志，若本模块发生入队或已接收到右侧模块的移位，则输出为 true
    val enq_Left_Entry  = Output(Valid(new PriorityEntry(priorityWidth, flowWidth)))  // 入队模式下，将本模块被新条目替换前的旧条目通过此端口移位给左侧模块
    val deq_Right_Entry = Output(Valid(new PriorityEntry(priorityWidth, flowWidth)))  // 出队模式下，本模块的出队输出（用于传递给右侧模块）

    val stored          = Output(new PriorityEntry(priorityWidth, flowWidth))
  })

  val entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth))

  io.stored               := entry
  io.shift_Flag_Out       := false.B
  io.enq_Left_Entry.valid := false.B
  io.deq_Right_Entry      := entry

  when(io.enqueue_Signal) {
    // 入队操作：局部决策逻辑
    // 只有当来自右侧模块未发生移位且新条目有效时，
    // 若新条目的优先级高于当前条目，则本模块锁存新条目，并将原条目作为移位数据输出给左侧模块。
    when(io.new_Entry.valid && !io.shift_Flag_In && (io.new_Entry.bits.priority > entry.priority)) {
      // 锁存新条目，同时输出原条目供左侧模块移位
      io.enq_Left_Entry.bits  := entry
      io.enq_Left_Entry.valid := true.B
      io.shift_Flag_Out       := true.B
      entry := io.new_Entry.bits
    } .elsewhen(io.shift_Flag_In) {
      // 如果右侧模块已锁存新条目，则本模块从右侧接收移位数据，实现条目向左移动
      io.enq_Left_Entry.bits  := entry
      io.enq_Left_Entry.valid := true.B
      io.shift_Flag_Out       := true.B
      entry := io.enq_Right_Entry.bits
    } .otherwise {
      // 否则保持当前条目不变
      entry := entry
      io.shift_Flag_Out := false.B
    }
  } .elsewhen(io.dequeue_Signal) {
    // 出队操作：模块0的条目将被读取，而其他模块则从左侧模块接收数据，实现条目向右移动
    io.deq_Right_Entry := entry
    entry := io.deq_Left_Entry
  } .otherwise {
    // 空闲时保持原值
    entry := entry
  }
}

class ShiftRegister(priorityWidth: Int, flowWidth: Int, depth: Int) extends Module {
  val io = IO(new Bundle{
    val control_signal = Input(UInt(1.W))
    val enqueue        = Input(new PriorityEntry(priorityWidth, flowWidth))
    val dequeue        = Output(new PriorityEntry(priorityWidth, flowWidth))
    val data_check     = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth)))
  })

  io.dequeue := PriorityEntry.default(priorityWidth, flowWidth)
  val current_elements = RegInit(VecInit(Seq.fill(depth)(PriorityEntry.default(priorityWidth, flowWidth))))
  val next_elements    = RegInit(VecInit(Seq.fill(depth)(PriorityEntry.default(priorityWidth, flowWidth))))
  val valid_elements   = RegInit(VecInit(Seq.fill(depth)(false.B)))
  val next_valid       = Wire(Vec(depth, Bool()))
  val count_index      = RegInit(0.U(log2Ceil(depth + 1).W))
  val max_index        = RegInit(0.U(log2Ceil(depth + 1).W))

  // 为入队操作计算“插入位置”
  // 对于每个位置 i，条件为：当前位置空 或者 新条目的优先级数值更大
  val insert_position = Wire(Vec(depth, Bool()))
  for(i <- 0 until depth) {
    insert_position(i) := !valid_elements(i) || (io.enqueue.priority > current_elements(i).priority)
  }
  val found  = insert_position.asUInt.orR             // 查看是否存在插入的可能
  val insIdx = PriorityEncoder(insert_position)       // 返回最右侧的可行的插入位置

  for(i <- 0 until depth) {
    next_elements(i) := current_elements(i)
    next_valid(i)    := valid_elements(i)
  }

  switch(io.control_signal) {
    is(0.U){                    // 控制信号0： 执行入队操作
      when(found){
        for(i <- 1 until depth){
          when(i.U < insIdx){
            next_elements(i) := current_elements(i)
            next_valid(i)    := valid_elements(i)
          }.elsewhen(i.U === insIdx){
            next_elements(i) := io.enqueue
            next_valid(i)    := true.B
          }.elsewhen(i.U > insIdx) {
            next_elements(i) := current_elements(i-1)
            next_valid(i)    := valid_elements(i-1)
          }
        }
      }.otherwise {
        next_elements := current_elements
        next_valid    := valid_elements
      }
    }
    is(1.U){
      io.dequeue := current_elements(max_index)
      count_index := count_index - 1.U
    }
  }
  io.data_check := next_elements
}
