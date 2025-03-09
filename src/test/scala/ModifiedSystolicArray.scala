package SystolicArray

import chisel3._
import chiseltest.RawTester.test
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ModifiedSystolicArray extends AnyFlatSpec{
  it should "ModifiedSystolicArrayPriorityQueue should enqueue and dequeue correctly" in {
    test(new ModifiedSystolicArrayBlock(8, 8, 8, 1)) { c =>
      // 测试样例1: 检测每个模块内是否可以完成正确的入队和出队操作
      // 开启入队模式
      c.io.block_enqueue_Signal.poke(true.B)
      c.io.block_dequeue_Signal.poke(false.B)
      c.clock.step()
      // 模拟一次入队操作：构造一个优先级为 10，flowId 为 3 的条目
      c.io.block_enqueue_entry.priority.poke(10.U)
      c.io.block_enqueue_entry.flowId.poke(3.U)
      c.io.block_enqueue_entry.subscript.poke(0.U)
      c.clock.step(4)

      c.io.block_enqueue_entry.priority.poke(11.U)
      c.io.block_enqueue_entry.flowId.poke(3.U)
      c.io.block_enqueue_entry.subscript.poke(0.U)
      c.clock.step(4)

      c.io.block_enqueue_entry.priority.poke(12.U)
      c.io.block_enqueue_entry.flowId.poke(3.U)
      c.io.block_enqueue_entry.subscript.poke(0.U)
      c.clock.step(2)
      c.io.block_enqueue_entry.priority.poke(12.U)
      c.io.block_enqueue_entry.flowId.poke(3.U)
      c.io.block_enqueue_entry.subscript.poke(1.U)
      c.clock.step(2)

      c.io.data_check(0).priority.expect(12.U)
      c.io.data_check(0).subscript.expect(0.U)
      c.io.data_check(1).priority.expect(12.U)
      c.io.data_check(1).subscript.expect(1.U)
      c.io.data_check(2).priority.expect(11.U)
      c.io.data_check(3).priority.expect(11.U)
      c.io.data_check(4).priority.expect(10.U)
      c.io.data_check(5).priority.expect(10.U)
      c.io.data_check(6).priority.expect(0.U)
    }
  }
}
