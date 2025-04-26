package fpga.pheap


import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import fpga._
import fpga.pheap.Const._

class BNodeTest extends AnyFreeSpec with ChiselScalatestTester {
  "PHeap should push entries and display all B nodes" in {
    test(new PHeap).withAnnotations(Seq(WriteVcdAnnotation)) { c =>

      def printHeap(): Unit = {
        println("\n==== Structured Heap View ====")
        var index = 0
        for (level <- 1 to count_of_levels) {
          val width = TreeIndexing.get_nodes_at_level(level)
          print(f"Level $level%2d: ")
          for (_ <- 0 until width) {
            val b = c.io.debug_B(index)
            val value = b.entry.rank.peek().litValue
            val active = b.entry.existing.peek().litToBoolean
            if (active) print(f"[$value%3d] ") else print("[ - ] ")
            index += 1
          }
          println()
        }
        println("==============================\n")
      }

      // Step: push a sequence of values
      val test_rank_values = Seq((7, 2), (13, 3), (2, 1), (23, 4))
      for ((v, u) <- test_rank_values) {
        c.io.op_in.pop.poke(true.B)
        c.io.op_in.push.rank.poke(v.U)
        c.io.op_in.push.existing.poke(true.B)
        c.io.op_in.push.metadata.poke(u.U)
        c.clock.step()

        c.io.op_in.pop.poke(false.B)
        c.io.op_in.push.rank.poke(v.U)
        c.io.op_in.push.existing.poke(false.B)
        c.io.op_in.push.metadata.poke(u.U)

        c.clock.step(5)
      }

      // Final wait for propagation
      c.clock.step(10)

      // Print all B nodes
      printHeap()
    }
  }
}
