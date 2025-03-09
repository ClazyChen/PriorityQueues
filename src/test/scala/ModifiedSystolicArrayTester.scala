package SystolicArray

import chisel3._
import chiseltest.RawTester.test
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ModifiedSystolicArrayTester extends AnyFlatSpec{
  it should "ModifiedSystolicArrayPriorityQueue should enqueue and dequeue correctly" in {
    test(new ModifiedSystolicArrayBlock(8, 8, 8, 1)) { c =>
      // 子任务1: 检测每个模块内是否可以完成正确的入队和出队操作
      c.io.data_check(0).priority.expect(255.U)
      c.io.data_check(1).priority.expect(255.U)
      c.io.data_check(2).priority.expect(255.U)

      // 开启入队模式
      c.io.block_enqueue_Signal.poke(true.B)
      c.io.block_dequeue_Signal.poke(false.B)

      // 模拟一次入队操作：构造一个优先级为 10，flowId 为 3 的条目
      c.io.block_enqueue_entry.priority.poke(10.U)
      c.io.block_enqueue_entry.flowId.poke(3.U)
      c.io.block_enqueue_entry.subscript.poke(0.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 11，flowId 为 4 的条目
      c.io.block_enqueue_entry.priority.poke(11.U)
      c.io.block_enqueue_entry.flowId.poke(4.U)
      c.io.block_enqueue_entry.subscript.poke(0.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 12，flowId 为 1，下标为0的条目
      c.io.block_enqueue_entry.priority.poke(12.U)
      c.io.block_enqueue_entry.flowId.poke(1.U)
      c.io.block_enqueue_entry.subscript.poke(0.U)
      c.clock.step(1)
      // 模拟一次入队操作：构造一个优先级为 12，flowId 为 4，下标为1的条目
      c.io.block_enqueue_entry.priority.poke(12.U)
      c.io.block_enqueue_entry.flowId.poke(4.U)
      c.io.block_enqueue_entry.subscript.poke(1.U)
      c.clock.step(1)


      c.io.data_check(0).priority.expect(10.U)
      c.io.data_check(1).priority.expect(11.U)

      c.io.data_check(2).priority.expect(12.U)
      c.io.data_check(2).subscript.expect(0.U)
      c.io.data_check(3).priority.expect(12.U)
      c.io.data_check(3).subscript.expect(1.U)

    }
    test(new ModifiedSystolicArrayPriorityQueue(8, 8, 4, 2, 1)) { c =>
      // 子任务2: 检测模块间的元素传递是否正确
      c.io.enqueue_Signal.poke(true.B)
      c.io.dequeue_Signal.poke(false.B)

      c.io.enqueue_Entry.priority.poke(14.U)
      c.io.enqueue_Entry.flowId.poke(8.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(2.U)
      c.io.enqueue_Entry.flowId.poke(6.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(3.U)
      c.io.enqueue_Entry.flowId.poke(5.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(7.U)
      c.io.enqueue_Entry.flowId.poke(6.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(22.U)
      c.io.enqueue_Entry.flowId.poke(6.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(17.U)
      c.io.enqueue_Entry.flowId.poke(6.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)
      c.io.enqueue_Signal.poke(false.B)
      c.io.dequeue_Signal.poke(true.B)

      c.io.dequeue_Entry.priority.expect(2.U)
      c.clock.step(1)
      c.io.dequeue_Entry.priority.expect(3.U)
      c.clock.step(1)
      c.io.dequeue_Entry.priority.expect(7.U)
      c.clock.step(1)
      c.io.dequeue_Entry.priority.expect(14.U)
      c.clock.step(1)
      c.io.dequeue_Entry.priority.expect(17.U)
      c.clock.step(1)
      c.io.dequeue_Entry.priority.expect(22.U)
      c.clock.step(1)


    }
    test(new ModifiedSystolicArrayPriorityQueue(8, 8, 4, 2, 1)) { c =>
      // 子任务3: 检测元素在模块间传递时是否能按下标顺序排列
      c.io.enqueue_Signal.poke(true.B)
      c.io.dequeue_Signal.poke(false.B)

      c.io.enqueue_Entry.priority.poke(1.U)
      c.io.enqueue_Entry.flowId.poke(1.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(1.U)
      c.io.enqueue_Entry.flowId.poke(5.U)
      c.io.enqueue_Entry.subscript.poke(1.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(2.U)
      c.io.enqueue_Entry.flowId.poke(1.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(3.U)
      c.io.enqueue_Entry.flowId.poke(1.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Entry.priority.poke(4.U)
      c.io.enqueue_Entry.flowId.poke(1.U)
      c.io.enqueue_Entry.subscript.poke(0.U)
      c.clock.step(1)

      c.io.enqueue_Signal.poke(false.B)
      c.io.dequeue_Signal.poke(true.B)

      c.io.dequeue_Entry.priority.expect(1.U)
      c.io.dequeue_Entry.subscript.expect(0.U)
      c.clock.step(1)

      c.io.dequeue_Entry.priority.expect(1.U)
      c.io.dequeue_Entry.subscript.expect(1.U)
      c.clock.step(1)

      c.io.dequeue_Entry.priority.expect(2.U)
      c.clock.step(1)

      c.io.dequeue_Entry.priority.expect(3.U)
      c.clock.step(1)

      c.io.dequeue_Entry.priority.expect(4.U)
      c.clock.step(1)
    }
  }
}
