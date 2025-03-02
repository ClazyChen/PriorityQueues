import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Test extends  AnyFlatSpec with ChiselScalatestTester {
    "ShiftRegister" should "correct" in {
        test(new ShiftRegister(16, 0, 10))
            .withAnnotations(Seq(WriteVcdAnnotation))  { dut =>

                // 插入10
            dut.io.read.poke(0.U)
            dut.io.write.poke(1.U)
            dut.io.newEntry.poke(10.U)
            dut.clock.step()
            dut.io.highestEntry.expect(10.U)

                // 插入30
            dut.io.read.poke(0.U)
            dut.io.write.poke(1.U)
            dut.io.newEntry.poke(30.U)
            dut.clock.step()
            dut.io.highestEntry.expect(10.U)

                // 弹出10
            dut.io.read.poke(1.U)
            dut.io.write.poke(0.U)
            dut.clock.step()
            dut.io.highestEntry.expect(30.U)

                // 插入20
            dut.io.read.poke(0.U)
            dut.io.write.poke(1.U)
            dut.io.newEntry.poke(20.U)
            dut.clock.step()
            dut.io.highestEntry.expect(20.U)
        }
    }
}