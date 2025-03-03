package ShiftRegister

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

// 假设你的模块文件包名为 ShiftRegister
class ShiftRegisterTest extends AnyFlatSpec {
  "ShiftRegister" should "正确完成入队和出队操作" in {
    // 使用优先级和流标号均为 8 位、深度为 4 的 ShiftRegister
    simulate(new ShiftRegister(8, 8, 4)) { c =>
      // 模拟入队操作：control_signal = 0.U 时进行入队
      // 入队第一个元素：优先级 10, 流标号 1
      c.io.control_signal.poke(0.U)
      c.io.enqueue.priority.poke(10.U)
      c.io.enqueue.flowId.poke(1.U)
      c.clock.step(1)

      c.io.control_signal.poke(0.U)
      c.io.enqueue.priority.poke(15.U)
      c.io.enqueue.flowId.poke(3.U)
      c.clock.step(1)

      c.io.data_check(0).priority.expect(10.U)
      c.io.data_check(0).flowId.expect(1.U)
      c.io.data_check(1).priority.expect(15.U)
      c.io.data_check(1).flowId.expect(3.U)
    }
  }
}

