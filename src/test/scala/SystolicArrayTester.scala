package SystolicArray

import chisel3._
import chiseltest.RawTester.test
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


class SystolicArrayTester extends AnyFlatSpec{
  it should "enqueue and dequeue correctly" in {
    // 使用 priorityWidth = 4, flowWidth = 4, depth = 4 作为测试参数
    test(new SystolicArray(4, 4, 4, 1)){ dut =>

      // 初始状态下，存储的 entry 应该为默认值 0
      dut.io.dequeue_Entry.priority.expect(0.U)
      dut.io.dequeue_Entry.flowId.expect(0.U)

      // 模拟一次入队操作：构造一个优先级为 5，flowId 为 1 的条目
      dut.io.enqueue_Entry.priority.poke(5.U)
      dut.io.enqueue_Entry.flowId.poke(1.U)
      dut.io.enqueue_Signal.poke(true.B)
      // 使能信号保持一周期以完成入队操作
      dut.clock.step(1)

      // 模拟一次入队操作：构造一个优先级为 6，flowId 为 12 的条目
      dut.io.enqueue_Entry.priority.poke(6.U)
      dut.io.enqueue_Entry.flowId.poke(12.U)
      dut.io.enqueue_Signal.poke(true.B)
      // 使能信号保持一周期以完成入队操作
      dut.clock.step(1)

      // 模拟一次入队操作：构造一个优先级为 3，flowId 为 4 的条目
      dut.io.enqueue_Entry.priority.poke(3.U)
      dut.io.enqueue_Entry.flowId.poke(4.U)
      dut.io.enqueue_Signal.poke(true.B)
      // 使能信号保持一周期以完成入队操作
      dut.clock.step(1)

      // 停止入队信号
      dut.io.enqueue_Signal.poke(false.B)

      dut.clock.step(4)

      dut.io.data_check(0).priority.expect(6)
      dut.io.data_check(0).flowId.expect(12)
      dut.io.data_check(1).priority.expect(5)
      dut.io.data_check(1).flowId.expect(1)
      dut.io.data_check(2).priority.expect(3)
      dut.io.data_check(2).flowId.expect(4)


      dut.io.dequeue_Entry.priority.expect(6)
      dut.io.dequeue_Entry.flowId.expect(12)

      // 模拟一次出队操作
      dut.io.dequeue_Signal.poke(true.B)
      dut.clock.step(1)

      dut.io.dequeue_Entry.priority.expect(5)
      dut.io.dequeue_Entry.flowId.expect(1)

      dut.io.dequeue_Signal.poke(false.B)
      dut.clock.step(4)


      // 模拟一次出队操作
      dut.io.dequeue_Signal.poke(true.B)
      dut.clock.step(1)

      dut.io.dequeue_Entry.priority.expect(3)
      dut.io.dequeue_Entry.flowId.expect(4)

      dut.io.dequeue_Signal.poke(false.B)
      dut.clock.step(4)
    }
  }
}

