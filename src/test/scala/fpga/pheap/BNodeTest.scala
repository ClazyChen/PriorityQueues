package fpga.pheap

import chisel3._
import chiseltest._
import fpga.Const.rank_width
import org.scalatest.freespec.AnyFreeSpec
import fpga._
import fpga.pheap.Const._

class BNodeTest extends AnyFreeSpec with ChiselScalatestTester {

  // ========================
  // 通用测试工具函数
  // ========================

  /** Push 一个元素 */
  def enqueue(dut: PHeap, rank: UInt, metadata: UInt): Unit = {
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(true.B)
    dut.io.op_in.push.metadata.poke(metadata)
    dut.io.op_in.push.rank.poke(rank)
    dut.clock.step()

    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step(2)
  }

  /** Pop 一个元素 */
  def dequeue(dut: PHeap): Unit = {
    dut.io.op_in.pop.poke(true.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step()

    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step(2)
  }

  /** NOP操作（空闲） */
  def nop(dut: PHeap, cycles: Int = 3): Unit = {
    dut.io.op_in.pop.poke(false.B)
    dut.io.op_in.push.existing.poke(false.B)
    dut.io.op_in.push.metadata.poke(0.U)
    dut.io.op_in.push.rank.poke((-1).S(rank_width.W).asUInt)
    dut.clock.step(cycles)
  }

  /** 打印堆结构 */
  def printHeap(dut: PHeap): Unit = {
    println("\n==== Structured Heap View ====")
    var index = 0
    for (level <- 1 to count_of_levels) {
      val width = TreeIndexing.get_nodes_at_level(level)
      print(f"Level $level%2d: ")
      for (_ <- 0 until width) {
        val b = dut.io.debug_B(index)
        val value = b.entry.rank.peek().litValue
        val capacity = b.capacity.peek().litValue
        val active = b.entry.existing.peek().litToBoolean
        if (active) print(f"[$value%3d, $capacity%3d] ") else print(f"[ - , $capacity%3d] ")
        index += 1
      }
      println()
    }
    println("==============================\n")
  }

  // ========================
  // 测试集
  // ========================

  "PHeap should support basic enqueue and print correctly" in {
    test(new PHeap).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // 测试数据: (rank, metadata)
      val testData = Seq(
        (7, 2), (4, 1), (5, 3)
      )

      // 连续push测试数据
      for ((rank, metadata) <- testData) {
        enqueue(dut, rank.U, metadata.U)
        nop(dut) // 插入后空闲几个周期
      }

      // 插入结束后，再等一会，确保数据稳定
      nop(dut)

      dequeue(dut)

      nop(dut, 5)

      // 打印当前堆结构
      printHeap(dut)
    }
  }
}
