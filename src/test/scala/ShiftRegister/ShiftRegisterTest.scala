package ShiftRegister

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

// 假设你的模块文件包名为 ShiftRegister
class ShiftRegisterTest extends AnyFlatSpec {
  "PriorityQueue" should "enqueue and dequeue entries correctly" in {
    simulate(new PriorityQueue(priorityWidth = 4, flowWidth = 8, depth = 4)) { c =>
      c.clock.step(2)

      for(i <- 0 until 4) {
        c.io.data_check(i).priority.expect(0.U)
        c.io.data_check(i).flowId.expect(0.U)
      }

      // --- 入队测试 ---
      // 提供一个有效的入队条目 (priority = 5, flowId = 100)
      c.io.enqueue_Entry.valid.poke(true.B)
      c.io.enqueue_Entry.bits.priority.poke(5.U)
      c.io.enqueue_Entry.bits.flowId.poke(100.U)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(1)

      // 检查：模块0应该存储新条目 (5, 100)
      c.io.data_check(0).priority.expect(5.U)
      c.io.data_check(0).flowId.expect(100.U)

      // 再次入队，但新条目的优先级较低，不应更新（priority = 3, flowId = 101）
      c.io.enqueue_Entry.valid.poke(true.B)
      c.io.enqueue_Entry.bits.priority.poke(3.U)
      c.io.enqueue_Entry.bits.flowId.poke(101.U)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(1)

      // 检查：模块0保持 (5, 100)
      c.io.data_check(0).priority.expect(5.U)
      c.io.data_check(0).flowId.expect(100.U)
      c.io.data_check(1).priority.expect(3.U)
      c.io.data_check(1).flowId.expect(101.U)


      // 入队一个更高优先级的条目 (priority = 6, flowId = 102)
      c.io.enqueue_Entry.valid.poke(true.B)
      c.io.enqueue_Entry.bits.priority.poke(6.U)
      c.io.enqueue_Entry.bits.flowId.poke(102.U)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(1)

      // 此时，依据逻辑，模块0将锁存新条目 (6,102)
      // 同时模块0原有条目 (5,100) 会通过移位信号传递给模块1
      c.io.data_check(0).priority.expect(6.U)
      c.io.data_check(0).flowId.expect(102.U)
      c.io.data_check(1).priority.expect(5.U)
      c.io.data_check(1).flowId.expect(100.U)

      c.io.enqueue_Entry.valid.poke(false.B)

      // --- 出队测试 ---
      // 触发出队操作
      c.io.dequeue_Signal.poke(true.B)
      // 对于模块 depth-1，其 deq_Left_Entry 已被置为默认值 (0,0)，故整个移位链将向右移动
      c.clock.step(1)

      // 检查：模块0会接收到来自模块1的条目，成为新的出队数据
      c.io.data_check(0).priority.expect(5.U)
      c.io.data_check(0).flowId.expect(100.U)
      // 同时，出队接口应反映模块0当前存储的条目
      c.io.dequeue_Entry.priority.expect(5.U)
      c.io.dequeue_Entry.flowId.expect(100.U)

      // 关闭出队使能
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(1)

    }
  }
}

