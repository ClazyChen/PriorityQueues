package SystolicArray

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

class SystolicArray {

}

