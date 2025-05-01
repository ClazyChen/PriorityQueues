package fpga.pheaptest

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import fpga.Const._
import fpga.pheaptest.Const._

class PHeapTest extends AnyFreeSpec with ChiselScalatestTester {
  def enqueue(dut: PHeap, rank: UInt, metadata: UInt): Unit = {
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(true.B)
    dut.io.op_in.push.rank.poke(rank)
    dut.io.op_in.push.metadata.poke(metadata)
    dut.clock.step()

    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.clock.step(2)
  }

  def dequeue(dut: PHeap): Unit = {
    dut.io.op_in.pop.poke(true.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.clock.step()

    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.clock.step(2)
  }

  def replace(dut: PHeap, rank: UInt, metadata: UInt): Unit = {
    dut.io.op_in.pop.poke(true.B)
    dut.io.op_in.push.existing.poke(true.B)
    dut.io.op_in.push.rank.poke(rank)
    dut.io.op_in.push.metadata.poke(metadata)
    dut.clock.step()

    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.clock.step(2)
  }

  def nop(dut: PHeap, cycles: Int = 3): Unit = {
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.clock.step(cycles)
  }

  "PHeap should support basic enqueue and dequeue" in {
    test(new PHeap).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      nop(dut, 9)

      val testData = Seq(
        (7, 4), (8, 1), (12, 3), (9, 5), (5, 5), (3, 12), (10, 8), (11, 8), (1, 2)
      )

      for((rank, metadata) <- testData) {
        enqueue(dut, rank.U, metadata.U)
        nop(dut, 12)
      }

      dut.io.op_in.pop.poke(true.B)
      dut.io.op_in.push.existing.poke(false.B)
      dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
      dut.io.op_in.push.metadata.poke(0.U)
      dut.io.entry_out.rank.expect(1.U)
      dut.clock.step()

      dut.io.op_in.pop.poke(false.B)
      dut.io.op_in.push.existing.poke(false.B)
      dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
      dut.io.op_in.push.metadata.poke(0.U)
      dut.clock.step(5)

      dut.io.op_in.pop.poke(true.B)
      dut.io.op_in.push.existing.poke(false.B)
      dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
      dut.io.op_in.push.metadata.poke(0.U)
      dut.io.entry_out.rank.expect(3.U)
      dut.clock.step()

      dut.io.op_in.pop.poke(false.B)
      dut.io.op_in.push.existing.poke(false.B)
      dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
      dut.io.op_in.push.metadata.poke(0.U)
      dut.clock.step(5)


    }
  }
}
