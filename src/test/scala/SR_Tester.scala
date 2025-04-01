package fpga

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.PriorityQueue
import fpga.Const._
import fpga.Entry._
import fpga.shiftregister._



class SR_Tester extends AnyFlatSpec with ChiselScalatestTester {

  // 封装入队操作：设置 push 有效，pop 和 replace 无效，并写入相应数据后步进一个时钟周期
  def enqueue(dut: ShiftRegister, rank: Int, metadata: Int): Unit = {
    dut.io.op_in.push.poke(true.B)
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.replace.poke(false.B)
    dut.io.op_in.entry_in.rank.poke(rank.U)
    dut.io.op_in.entry_in.metadata.poke(metadata.U)
    dut.io.op_in.entry_in.existing.poke(true.B)
    dut.clock.step()
  }

  // 封装出队操作：设置 pop 有效，push 和 replace 无效，然后步进一个时钟周期
  def dequeue(dut: ShiftRegister): Unit = {
    dut.io.op_in.push.poke(false.B)
    dut.io.op_in.pop.poke(true.B)
    dut.io.op_in.replace.poke(false.B)
    dut.clock.step()
  }

  // 封装替换操作：设置 push 有效，pop 和 replace 无效，并写入相应数据后步进一个时钟周期
  def replace(dut: ShiftRegister, rank: Int, metadata: Int): Unit = {
    dut.io.op_in.push.poke(true.B)
    dut.io.op_in.pop.poke(true.B)
    dut.io.op_in.replace.poke(false.B)
    dut.io.op_in.entry_in.rank.poke(rank.U)
    dut.io.op_in.entry_in.metadata.poke(metadata.U)
    dut.io.op_in.entry_in.existing.poke(true.B)
    dut.clock.step()

  }

  "PriorityQueue" should "enqueue and dequeue entries correctly" in {
    test(new ShiftRegister) { dut =>
      // 依次执行入队操作，入队三个数据（注意：具体顺序应与模块内部优先级逻辑相对应）
      enqueue(dut, 46, 0)
      enqueue(dut, 48, 0)
      enqueue(dut, 44, 0)
      enqueue(dut, 43, 0)
      enqueue(dut, 49, 0)
      enqueue(dut, 12, 0)

      dut.io.entry_out.rank.expect(12.U)

      dequeue(dut)
      dut.io.entry_out.rank.expect(43.U)

    }
  }
}
