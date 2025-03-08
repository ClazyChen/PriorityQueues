import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


class SAtest extends AnyFlatSpec with ChiselScalatestTester {
  def enqueue(dut: SystolicArray, rank: Int): Unit = {
    dut.io.read.poke(false.B)
    dut.io.write.poke(true.B)
    dut.io.new_entry.rank.poke(rank.U)
    dut.clock.step()
  }

  def dequeue(dut: SystolicArray): Unit = {
    dut.io.read.poke(true.B)
    dut.io.write.poke(false.B)
    dut.clock.step(2) // dequeue需要两个周期
  }

  def hazard(dut: SystolicArray) = {
    dut.io.read.poke(true.B)
    dut.io.write.poke(true.B)
    dut.io.new_entry.rank.poke(1.U)
    dut.clock.step()
  }

  "SystolicArray" should "pass" in {
    test(new SystolicArray(0, 4, 4)) { dut =>
      dut.io.output.rank.expect(15.U)

      // 1
      enqueue(dut, 1)
      dut.io.output.rank.expect(1.U)

      // 4, 1
      enqueue(dut, 4)
      dut.io.output.rank.expect(1.U)

      // 4
      dequeue(dut)
      dut.io.output.rank.expect(4.U)

      // 5, 4
      enqueue(dut, 5)
      dut.io.output.rank.expect(4.U)

      // 5, 4, 2
      enqueue(dut, 2)
      dut.io.output.rank.expect(2.U)

      // 5, 4, 2
      hazard(dut)
      dut.io.output.rank.expect(2.U)

      // 5, 4, 2, 1
      enqueue(dut, 1)
      dut.io.output.rank.expect(1.U)

      // 4, 2, 1, 0
      enqueue(dut, 0)
      dut.io.output.rank.expect(0.U)

      // 4, 2, 1
      dequeue(dut)
      dut.io.output.rank.expect(1.U)

      // 4, 2
      dequeue(dut)
      dut.io.output.rank.expect(2.U)

      // 4
      dequeue(dut)
      dut.io.output.rank.expect(4.U)

      // null, 默认优先级是b1111
      dequeue(dut)
      dut.io.output.rank.expect(15.U)
    }
  }
}
