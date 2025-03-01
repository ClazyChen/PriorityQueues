package ShiftRegister

import chisel3._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.simulator.EphemeralSimulator._

class ShiftRegisterTest extends AnyFlatSpec{
  "ShiftRegister" should "保持状态 when control = 0" in{
    simulate(new ShiftRegister(4, 8)){ dut =>
      dut.io.load.poke("h44332211".U)
      dut.io.control.poke(3.U)
      dut.clock.step(1)

      dut.io.in.poke("hAA".U)
      dut.io.control.poke(0.U)
      dut.clock.step(1)

      dut.io.out.expect("h44".U)
    }
  }
  "ShiftRegister" should "向左移位 when control = 1" in{
    simulate(new ShiftRegister(4, 8)){ dut =>
      dut.io.load.poke("hAABBCCDD".U)
      dut.io.control.poke(3.U)
      dut.clock.step(1)

      dut.io.control.poke(1.U)
      dut.io.in.poke("hAA".U)
      dut.clock.step(1)
      dut.io.control.poke(1.U)
      dut.io.in.poke("hBB".U)
      dut.clock.step(1)
      dut.io.dataOut(0).expect("hBB".U)
      dut.io.dataOut(1).expect("hAA".U)
      dut.io.dataOut(2).expect("hDD".U)
      dut.io.dataOut(3).expect("hCC".U)

      dut.io.out.expect("hCC".U)
    }
  }
  "ShiftRegister" should "向右移动 when control = 2" in {
      simulate(new ShiftRegister(4, 8)) { dut =>
        dut.io.load.poke("h44332211".U)
        dut.io.control.poke(3.U)
        dut.clock.step(1)


        dut.io.control.poke(2.U)
        dut.io.in.poke("hFF".U)
        dut.clock.step(1)
        dut.io.dataOut(0).expect("h22".U)
        dut.io.dataOut(1).expect("h33".U)
        dut.io.dataOut(2).expect("h44".U)
        dut.io.dataOut(3).expect("hFF".U)

        dut.io.out.expect("hFF".U)
      }
  }
}
