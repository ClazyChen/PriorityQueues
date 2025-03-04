package ShiftRegister

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

// 假设你的模块文件包名为 ShiftRegister
class ShiftRegisterTest extends AnyFlatSpec {
  "PriorityQueue" should "enqueue and dequeue entries correctly" in {
    simulate(new PriorityQueue(priorityWidth = 4, flowWidth = 4, depth = 2)) { c =>
      c.clock.step(2)


      // 入队第1个条目：priority = 6, flowId = 1
      c.io.enqueue_Entry.valid.poke(true.B)
      c.io.enqueue_Entry.bits.priority.poke(6.U)
      c.io.enqueue_Entry.bits.flowId.poke(1.U)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(5)
      // 入队第1个条目：priority = 5, flowId = 2
      c.io.enqueue_Entry.valid.poke(true.B)
      c.io.enqueue_Entry.bits.priority.poke(5.U)
      c.io.enqueue_Entry.bits.flowId.poke(2.U)
      c.io.dequeue_Signal.poke(false.B)

      c.clock.step(5)
      // 执行出队操作
      c.io.dequeue_Signal.poke(true.B)
      c.clock.step()
      // 断言出队输出正确（最高优先级条目: priority = 6, flowId = 1）
      c.io.dequeue_Entry.priority.expect(6.U)
      c.io.dequeue_Signal.poke(false.B)
      c.io.dequeue_Entry.flowId.expect(1.U)
      c.clock.step(5)
      // 执行出队操作
      c.io.dequeue_Signal.poke(true.B)
      c.clock.step()
      // 断言出队输出正确（最高优先级条目: priority = 5, flowId = 2）
      c.io.dequeue_Entry.priority.expect(5.U)
      c.io.dequeue_Entry.flowId.expect(2.U)
      c.clock.step(5)


    }
  }
}

