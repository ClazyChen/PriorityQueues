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

    // 清除输入，防止悬挂
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
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
    test(new PHeap).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>


    }
  }
}