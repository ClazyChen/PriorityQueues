package ShiftRegister

import chisel3._
import chiseltest.RawTester.test
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


class ShiftRegisterTest extends AnyFlatSpec {
  "PriorityQueue" should "enqueue and dequeue entries correctly" in {
    test(new ShiftRegister(priorityWidth = 8, flowWidth = 8, depth = 16)) { c =>
      // 模拟入队操作
      c.io.enqueue_Signal.poke(true.B)
      c.io.dequeue_Signal.poke(false.B)

      // 模拟一次入队操作：构造一个优先级为 5，flowId 为 1 的条目
      c.io.enqueue_Entry.priority.poke(5.U)
      c.io.enqueue_Entry.flowId.poke(1.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 34，flowId 为 36 的条目
      c.io.enqueue_Entry.priority.poke(34.U)
      c.io.enqueue_Entry.flowId.poke(36.U)
      c.clock.step(1)

      // 模拟一次入队操作：构造一个优先级为 108，flowId 为 33 的条目
      c.io.enqueue_Entry.priority.poke(108.U)
      c.io.enqueue_Entry.flowId.poke(33.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 54，flowId 为 54 的条目
      c.io.enqueue_Entry.priority.poke(54.U)
      c.io.enqueue_Entry.flowId.poke(54.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 1，flowId 为 90 的条目
      c.io.enqueue_Entry.priority.poke(1.U)
      c.io.enqueue_Entry.flowId.poke(90.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 52，flowId 为 77 的条目
      c.io.enqueue_Entry.priority.poke(52.U)
      c.io.enqueue_Entry.flowId.poke(77.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 67，flowId 为 56 的条目
      c.io.enqueue_Entry.priority.poke(67.U)
      c.io.enqueue_Entry.flowId.poke(56.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 74，flowId 为 48 的条目
      c.io.enqueue_Entry.priority.poke(74.U)
      c.io.enqueue_Entry.flowId.poke(48.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 43，flowId 为 71 的条目
      c.io.enqueue_Entry.priority.poke(43.U)
      c.io.enqueue_Entry.flowId.poke(71.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 91，flowId 为 64 的条目
      c.io.enqueue_Entry.priority.poke(91.U)
      c.io.enqueue_Entry.flowId.poke(64.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 88，flowId 为 87 的条目
      c.io.enqueue_Entry.priority.poke(88.U)
      c.io.enqueue_Entry.flowId.poke(87.U)
      c.clock.step(1)

      c.io.enqueue_Signal.poke(false.B)
      c.io.dequeue_Signal.poke(false.B)

      c.clock.step(16)

      c.io.block_check(10).priority.expect(108.U)
      c.io.block_check(10).flowId.expect(33.U)
      c.io.block_check(9).priority.expect(91.U)
      c.io.block_check(9).flowId.expect(64.U)
      c.io.block_check(8).priority.expect(88.U)
      c.io.block_check(8).flowId.expect(87.U)
      c.io.block_check(7).priority.expect(74.U)
      c.io.block_check(7).flowId.expect(48.U)
      c.io.block_check(6).priority.expect(67.U)
      c.io.block_check(6).flowId.expect(56.U)
      c.io.block_check(5).priority.expect(54.U)
      c.io.block_check(5).flowId.expect(54.U)
      c.io.block_check(4).priority.expect(52.U)
      c.io.block_check(4).flowId.expect(77.U)
      c.io.block_check(3).priority.expect(43.U)
      c.io.block_check(3).flowId.expect(71.U)
      c.io.block_check(2).priority.expect(34.U)
      c.io.block_check(2).flowId.expect(36.U)
      c.io.block_check(1).priority.expect(5.U)
      c.io.block_check(1).flowId.expect(1.U)
      c.io.block_check(0).priority.expect(1.U)
      c.io.block_check(0).flowId.expect(90.U)

      c.io.enqueue_Signal.poke(false.B)
      c.io.dequeue_Signal.poke(true.B)

      c.io.dequeue_Entry.priority.expect(1.U)
      c.io.dequeue_Entry.flowId.expect(90.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(5.U)
      c.io.dequeue_Entry.flowId.expect(1.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(34.U)
      c.io.dequeue_Entry.flowId.expect(36.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(43.U)
      c.io.dequeue_Entry.flowId.expect(71.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(52.U)
      c.io.dequeue_Entry.flowId.expect(77.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(54.U)
      c.io.dequeue_Entry.flowId.expect(54.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(67.U)
      c.io.dequeue_Entry.flowId.expect(56.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(74.U)
      c.io.dequeue_Entry.flowId.expect(48.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(88.U)
      c.io.dequeue_Entry.flowId.expect(87.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(91.U)
      c.io.dequeue_Entry.flowId.expect(64.U)
      c.clock.step()

      c.io.dequeue_Entry.priority.expect(108.U)
      c.io.dequeue_Entry.flowId.expect(33.U)
      c.clock.step()
    }
  }
}