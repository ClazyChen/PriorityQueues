package ShiftRegister

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

class ShiftRegisterTest extends AnyFlatSpec{
  "ShiftRegister" should "shift correctly" in{
    simulate(new ShiftRegister(4)){ dut =>
      dut.io.in.poke(1)
      dut.clock.step(1)
      dut.io.out.expect("b0001".U)

      dut.io.in.poke(0)
      dut.clock.step(1)
      dut.io.out.expect("b0010".U)

      dut.io.in.poke(1)
      dut.clock.step(1)
      dut.io.out.expect("b0101".U)
    }
  }
}
