package fpga.pheap

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import fpga.pheap._

class PHeapMemTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "PHeapMemory"
  it should "correctly write and read at different levels and addresses" in {
    test(new PHeapMem("Sream")) { c =>
      val test_data = Seq(
        (1, 0, 0x11),
        (2, 1, 0x22),
        (3, 2, 0x33)
      )

      // initialize the memory module
      c.io.read_signal.poke(false.B)
      c.io.write_signal.poke(false.B)
      c.clock.step(2)

      for((level, addr, value) <- test_data) {
        c.io.operation_level.poke(level.U)
        c.io.operation_local_addr.poke(addr.U)
        c.io.data_in.poke(value.U)
        c.io.write_signal.poke(true.B)
        c.clock.step()

        c.io.write_signal.poke(false.B)
        c.clock.step()

        c.io.read_signal.poke(true.B)
        c.clock.step()
        c.io.data_out.expect(value.U, s"Level &level Addr &addr should return &value")

        c.io.read_signal.poke(false.B)
        c.clock.step()
      }
    }
  }

}
