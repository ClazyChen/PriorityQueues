package fpga.pheap

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import fpga._
import fpga.Const._

class PHeapTester extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "PHeap"

  def enqueue(dut: PHeap, metadata: UInt, rank: UInt): Unit = {
    // 第一个周期发起 push 操作
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(true.B)
    dut.io.op_in.push.metadata.poke(metadata)
    dut.io.op_in.push.rank.poke(rank)
    dut.clock.step()

    // 清空 push 控制信号，防止残留
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank.getWidth.W).asUInt)
    dut.clock.step(2)
  }

  def dequeue(dut: PHeap): Unit = {
    // 第一个周期发起 pop 操作
    dut.io.op_in.pop.poke(true.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step()

    // 清除输入，防止悬挂
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step(2)
  }

  def nop(dut: PHeap): Unit = {
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step(3)
  }
  it should "perform push and pop operations correctly" in {
    test(new PHeap) { dut =>

      enqueue(dut, 3.U, 13.U)
      nop(dut)
      enqueue(dut, 4.U, 15.U)
      nop(dut)
      dut.io.entry_out.rank.expect(15.U)
      dequeue(dut)
      nop(dut)
      dut.io.entry_out.rank.expect(13.U)
    }
  }
}