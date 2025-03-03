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
    }
  }
}

