import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SRtest extends AnyFlatSpec with ChiselScalatestTester {
  def debugPrint(dut: ShiftRegister, tag: String): Unit = {
    val entries = dut.io.dbgPort.get.map(_.rank.peek().litValue)
    println(f"${tag}%-8s: ${entries.mkString("[", ", ", "]")}")
  }

  def enqueue(dut: ShiftRegister, rank: Int): Unit = {
    dut.io.read_in.poke(false.B)
    dut.io.write_in.poke(true.B)
    dut.io.new_entry_in.rank.poke(rank.U)
    dut.clock.step()
    debugPrint(dut, s"enq(${rank})")
  }

  def dequeue(dut: ShiftRegister): Unit = {
    dut.io.read_in.poke(true.B)
    dut.io.write_in.poke(false.B)
    dut.clock.step()
    debugPrint(dut, "deq")
  }

  def replace(dut: ShiftRegister, rank: Int): Unit = {
    dut.io.read_in.poke(true.B)
    dut.io.write_in.poke(true.B)
    dut.io.new_entry_in.rank.poke(rank.U)
    dut.clock.step()
    debugPrint(dut, s"rep(${rank})")
  }

  // 原测试代码基础上增加调试输出调用
  "ShiftRegister" should "pass" in {
    test(new ShiftRegister(0, 4, 4, debug = true)) { dut =>  // 添加debug参数
      debugPrint(dut, "Init")     // 初始状态输出
      dut.io.output_out.rank.expect(15.U)

      enqueue(dut, 1)
      dut.io.output_out.rank.expect(1.U)

      enqueue(dut, 4)
      dut.io.output_out.rank.expect(1.U)

      dequeue(dut)
      dut.io.output_out.rank.expect(4.U)

      enqueue(dut, 5)
      dut.io.output_out.rank.expect(4.U)

      enqueue(dut, 2)
      dut.io.output_out.rank.expect(2.U)

      enqueue(dut, 7)
      dut.io.output_out.rank.expect(2.U)

      replace(dut, 3)
      dut.io.output_out.rank.expect(2.U)

      replace(dut, 6)
      dut.io.output_out.rank.expect(3.U)

      replace(dut, 1)
      dut.io.output_out.rank.expect(1.U)
    }
  }
}
