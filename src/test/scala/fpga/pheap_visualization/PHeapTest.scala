package fpga.pheap_visualization

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import fpga.Const._
import fpga.pheap_visualization.Const._

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

  def printPHeap(dut: PHeap): Unit = {
    println("\n============ Structured Pheap View ============")
    var index = 0
    for (level <- 1 to count_of_levels) {
      val width = TreeIndexing.get_nodes_at_level(level)
      print(f"Level $level%2d: ")
      for(_ <- 0 until width) {
        val node = dut.io.debug_B(index)
        val value = node.entry.rank.peek().litValue
        val capacity = node.capacity.peek().litValue
        val active = node.entry.existing.peek().litToBoolean
        if(active) print(f"[$value%3d, $capacity%3d] ") else print(f"[ - , $capacity%3d] ")
        index += 1
      }
      println()
    }
    println("====================================\n")
  }

  "PHeap should support basic enqueue and dequeue" in {
    test(new PHeap).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      printPHeap(dut)
      nop(dut, 9)

      val testData = Seq(
        (7, 4), (8, 1), (12, 3), (9, 5), (5, 5), (3, 12), (10, 8), (11, 8), (1, 2)
      )

      for((rank, metadata) <- testData) {
        enqueue(dut, rank.U, metadata.U)
        nop(dut, 12)
        println("======================================")
        print(f"[push entry = $rank%3d, $metadata%3d]\n")
        println("======================================")
        printPHeap(dut)
      }

      println("======================================")
      print(f"[pop\n]")
      println("======================================")
      dequeue(dut)
      nop(dut, 12)
      printPHeap(dut)

      println("======================================")
      print(f"[pop\n")
      println("======================================")
      dequeue(dut)
      nop(dut, 12)
      printPHeap(dut)

      println("======================================")
      print(f"[pop\n]")
      println("======================================")
      dequeue(dut)
      nop(dut, 12)
      printPHeap(dut)

      println("======================================")
      print(f"[pop\n]")
      println("======================================")
      replace(dut, 11.U, 5.U)
      nop(dut, 12)
      printPHeap(dut)
    }
  }
}
