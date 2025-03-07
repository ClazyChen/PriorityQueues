package SystolicArray
import chisel3._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

class ModifiedSystolicArray extends AnyFlatSpec{
  it should "ModifiedSystolicArrayBlock should enqueue and dequeue correctly" in {

    simulate(new ModifiedSystolicArrayBlock(4, 4, 4)) { c =>
      c.clock.step(2)

      for(i <- 0 until 4) {
        c.io.data_check(i).priority.expect(0.U)
        c.io.data_check(i).flowId.expect(0.U)
      }
      c.clock.step(1)

      c.io.block_enqueue_Signal.poke(true.B)

      c.io.block_enqueue_entry.priority.poke(10.U)
      c.io.block_enqueue_entry.flowId.poke(3.U)
      c.clock.step(1)

      c.io.data_check(0).priority.expect(10.U)
      c.io.data_check(0).flowId.expect(3.U)

      c.io.block_enqueue_entry.priority.poke(7.U)
      c.io.block_enqueue_entry.flowId.poke(4.U)
      c.clock.step(1)

      c.io.data_check(0).priority.expect(10.U)
      c.io.data_check(0).flowId.expect(3.U)
      c.io.data_check(1).priority.expect(7.U)
      c.io.data_check(1).flowId.expect(4.U)

      c.io.block_enqueue_Signal.poke(false.B)
      c.io.block_dequeue_Signal.poke(true.B)
      c.clock.step(1)

      c.io.data_check(0).priority.expect(7.U)
      c.io.data_check(0).flowId.expect(4.U)

    }
  }
}
