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
  val count_index      = RegInit(0.U(log2Ceil(depth + 1).W))
  val max_index        = RegInit(0.U(log2Ceil(depth).W))

  switch(io.control_signal) {
    is(0.U){                    // 控制信号0： 执行入队操作
      current_elements(count_index) := io.enqueue
      count_index := count_index + 1.U
    }
    is(1.U){
      io.dequeue := current_elements(0)
      count_index := count_index - 1.U
    }
  }
  io.data_check := current_elements
}
