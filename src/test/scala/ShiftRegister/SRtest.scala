import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SRtest extends AnyFlatSpec with ChiselScalatestTester {
    "ShiftRegister" should "pass" in {
        test(new ShiftRegister(0, 4, 4)) { dut =>
            dut.io.output.expect("b1111".U)

            // enqueue(1)
            dut.io.read.poke(false.B)
            dut.io.write.poke(true.B)
            dut.io.new_entry.poke(1.U)
            dut.clock.step()
            dut.io.output.expect(1.U)

            // enqueue(4)
            dut.io.read.poke(false.B)
            dut.io.write.poke(true.B)
            dut.io.new_entry.poke(4.U)
            dut.clock.step()
            dut.io.output.expect(1.U)

            // dequeue(1)
            dut.io.read.poke(true.B)
            dut.io.write.poke(false.B)
            dut.io.new_entry.poke(1.U)
            dut.clock.step()
            dut.io.output.expect(4.U)

            // enqueue(5)
            dut.io.read.poke(false.B)
            dut.io.write.poke(true.B)
            dut.io.new_entry.poke(5.U)
            dut.clock.step()
            dut.io.output.expect(4.U)

            // enqueue(2)
            dut.io.read.poke(false.B)
            dut.io.write.poke(true.B)
            dut.io.new_entry.poke(2.U)
            dut.clock.step()
            dut.io.output.expect(2.U)

            // hazard
            dut.io.read.poke(true.B)
            dut.io.write.poke(true.B)
            dut.io.new_entry.poke(1.U)
            dut.clock.step()
            dut.io.output.expect(2.U)
        }
    }
}