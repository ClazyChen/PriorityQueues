package fpga.mem

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SramSpec extends AnyFlatSpec with ChiselScalatestTester with Matchers {
    behavior of "Sram"

    it should "perform basic read and write operations" in {
        test(new Sram(16, 32)) { dut =>
            // initial state should be idle
            dut.io.r.en.poke(false.B)
            dut.io.w.en.poke(false.B)
            
            // write data
            dut.io.w.en.poke(true.B)
            dut.io.w.addr.poke(0.U)
            dut.io.w.data.poke(0x12345678.U)
            dut.clock.step()
            
            // read data
            dut.io.w.en.poke(false.B)
            dut.io.r.en.poke(true.B)
            dut.io.r.addr.poke(0.U)
            dut.clock.step() // sram read has one cycle delay
            dut.io.r.data.expect(0x12345678.U)
        }
    }

    it should "handle read-write conflicts correctly" in {
        test(new Sram(16, 32)) { dut =>
            // initial state
            dut.io.r.en.poke(false.B)
            dut.io.w.en.poke(false.B)
            
            // write initial data
            dut.io.w.en.poke(true.B)
            dut.io.w.addr.poke(1.U)
            dut.io.w.data.poke(0x11111111.U)
            dut.clock.step()
            
            // read and write to the same address simultaneously
            dut.io.r.en.poke(true.B)
            dut.io.w.en.poke(true.B)
            dut.io.r.addr.poke(1.U)
            dut.io.w.addr.poke(1.U)
            dut.io.w.data.poke(0x22222222.U)
            dut.clock.step()
            
            // check read result (should return the newly written data)
            dut.io.r.data.expect(0x22222222.U)
        }
    }

    it should "handle multiple operations correctly" in {
        test(new Sram(16, 32)) { dut =>
            // write to multiple addresses
            for (i <- 0 until 4) {
                dut.io.w.en.poke(true.B)
                dut.io.w.addr.poke(i.U)
                dut.io.w.data.poke((0x10000000 + i).U)
                dut.clock.step()
            }
            
            // read from multiple addresses
            for (i <- 0 until 4) {
                dut.io.w.en.poke(false.B)
                dut.io.r.en.poke(true.B)
                dut.io.r.addr.poke(i.U)
                dut.clock.step()
                dut.io.r.data.expect((0x10000000 + i).U)
            }
        }
    }

    it should "handle idle state correctly" in {
        test(new Sram(16, 32)) { dut =>
            // write some data
            dut.io.w.en.poke(true.B)
            dut.io.w.addr.poke(0.U)
            dut.io.w.data.poke(0x12345678.U)
            dut.clock.step()
            
            // enter idle state
            dut.io.r.en.poke(false.B)
            dut.io.w.en.poke(false.B)
            dut.clock.step()
            
            // reading again should still get the previously written data
            dut.io.r.en.poke(true.B)
            dut.io.r.addr.poke(0.U)
            dut.clock.step()
            dut.io.r.data.expect(0x12345678.U)
        }
    }
} 