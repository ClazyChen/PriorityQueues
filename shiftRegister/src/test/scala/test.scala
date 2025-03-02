import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Test extends  AnyFlatSpec with ChiselScalatestTester {
    "ShiftRegister" should "correct" in {
        test(new ShiftRegister)
            .withAnnotations(Seq(WriteVcdAnnotation))  { dut =>
            dut.io.write.poke(1.U)
            dut.io.newEntry.poke(10.U)
            dut.clock.step()
            dut.io.highestEntry.expect(10.U)
            dut.io.write.poke(0.U)
            dut.io.read.poke(1.U)
            dut.clock.step()
            dut.io.highestEntry.expect(65535.U)

        }
    }
}