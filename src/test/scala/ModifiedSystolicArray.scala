package SystolicArray
import chisel3._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

class ModifiedSystolicArray extends AnyFlatSpec{
  it should "ModifiedSystolicArrayPriorityQueue should enqueue and dequeue correctly" in {
    simulate(new ModifiedSystolicArrayPriorityQueue(8, 8, 4, 2)) { c =>
      c.clock.step(2)
      // -----------------------------------------------------------------
      // 测试样例1: 检测每个模块内是否可以完成正确的入队和出队操作
      // 开启入队模式
      c.io.enqueue_Signal.poke(true.B)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 10，flowId 为 3 的条目
      c.io.enqueue_Entry.priority.poke(10.U)
      c.io.enqueue_Entry.flowId.poke(3.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 12，flowId 为 4 的条目
      c.io.enqueue_Entry.priority.poke(12.U)
      c.io.enqueue_Entry.flowId.poke(4.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 9，flowId 为 6 的条目
      c.io.enqueue_Entry.priority.poke(9.U)
      c.io.enqueue_Entry.flowId.poke(6.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 17，flowId 为 7 的条目
      c.io.enqueue_Entry.priority.poke(17.U)
      c.io.enqueue_Entry.flowId.poke(7.U)
      c.clock.step()
      // 停止入队操作，此时优先级队列中的元素依次为(17, 7) (12, 4) (10, 3) (9, 6)
      c.io.enqueue_Signal.poke(false.B)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(4)
      // 开启出队模式
      c.io.dequeue_Entry.priority.expect(17.U)
      c.io.dequeue_Entry.flowId.expect(7.U)
      c.io.dequeue_Signal.poke(true.B)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(12.U)
      c.io.dequeue_Entry.flowId.expect(4.U)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(10.U)
      c.io.dequeue_Entry.flowId.expect(3.U)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(9.U)
      c.io.dequeue_Entry.flowId.expect(6.U)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(0.U)
      c.io.dequeue_Entry.flowId.expect(0.U)

      c.clock.step(4)
    }
    simulate(new ModifiedSystolicArrayPriorityQueue(8, 8, 4, 2)) { c =>
      // -----------------------------------------------------------------
      // 测试样例2: 检测队列是否可以正确完成模块间的传递
      // 开启入队模式
      c.io.enqueue_Signal.poke(true.B)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 10，flowId 为 3 的条目
      c.io.enqueue_Entry.priority.poke(10.U)
      c.io.enqueue_Entry.flowId.poke(3.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 12，flowId 为 4 的条目
      c.io.enqueue_Entry.priority.poke(12.U)
      c.io.enqueue_Entry.flowId.poke(4.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 9，flowId 为 6 的条目
      c.io.enqueue_Entry.priority.poke(9.U)
      c.io.enqueue_Entry.flowId.poke(6.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 17，flowId 为 7 的条目
      c.io.enqueue_Entry.priority.poke(17.U)
      c.io.enqueue_Entry.flowId.poke(7.U)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 6，flowId 为 8 的条目
      c.io.enqueue_Entry.priority.poke(6.U)
      c.io.enqueue_Entry.flowId.poke(8.U)
      c.clock.step()

      // 停止入队操作，此时优先级队列中的元素依次为(17, 7) (12, 4) (10, 3) (9, 6) ｜ (6, 8)
      c.io.enqueue_Signal.poke(false.B)
      c.io.dequeue_Signal.poke(false.B)
      c.clock.step(4)
      // 开启出队模式
      c.io.dequeue_Entry.priority.expect(17.U)
      c.io.dequeue_Entry.flowId.expect(7.U)
      c.io.dequeue_Signal.poke(true.B)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(12.U)
      c.io.dequeue_Entry.flowId.expect(4.U)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(10.U)
      c.io.dequeue_Entry.flowId.expect(3.U)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(9.U)
      c.io.dequeue_Entry.flowId.expect(6.U)
      c.clock.step()
      c.io.dequeue_Entry.priority.expect(6.U)
      c.io.dequeue_Entry.flowId.expect(8.U)
    }
  }
}
