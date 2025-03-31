import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.PriorityQueue
import fpga._
import fpga.Const._
import fpga.sa._


// 认为测试有待讨论，所以先不修改BlackBox的代码，还是用之前的简单版测试
class SATest extends  AnyFlatSpec with ChiselScalatestTester {
    def push(dut: SystolicArray, rank: Int, expectRank: Int): Unit = {
        dut.io.op_in.push.existing.poke(true.B)
        dut.io.op_in.push.rank.poke(rank.U)
        dut.io.op_in.push.metadata.poke(0)
        dut.io.op_in.pop.poke(false.B)
   //     println(s"push   : ${dut.io.op_in.push.rank.peek.litValue.toInt.toHexString}")
        dut.clock.step()
        dut.io.entry_out.rank.expect(expectRank.U)
    }

    def pop(dut: SystolicArray, expectRank: Int): Unit = {
        dut.io.op_in.push.existing.poke(false.B)
        dut.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
        dut.io.op_in.push.metadata.poke(0)
        dut.io.op_in.pop.poke(true.B)
        dut.io.entry_out.rank.expect(expectRank.U)
    //    println(s"pop    : ${dut.io.entry_out.rank.peek.litValue.toInt.toHexString}")
        dut.clock.step()
    }

    def replace(dut: SystolicArray, rank: Int, expectRank: Int): Unit = {
        dut.io.op_in.push.existing.poke(true.B)
        dut.io.op_in.push.rank.poke(rank.U)
        dut.io.op_in.push.metadata.poke(0)
        dut.io.op_in.pop.poke(true.B)
     //   println(s"replace: ${dut.io.op_in.push.rank.peek.litValue.toInt.toHexString}")
        dut.clock.step()
        // 输入没改变，可能导致输出也没变，所以等nop再检测
        //dut.io.entry_out.rank.expect(expectRank.U)
    }

    def nop(dut: SystolicArray, expectRank: Int): Unit = {
        dut.io.op_in.push.existing.poke(false.B)
        dut.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
        dut.io.op_in.push.metadata.poke(0)
        dut.io.op_in.pop.poke(false.B)
        dut.clock.step()
        dut.io.entry_out.rank.expect(expectRank.U)
    }

    "SystolicArray" should "correct" in {
        test(new SystolicArray)
            .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
                // 用自带的优先级队列测试
                val minPriorityQueue = PriorityQueue[Int]()(Ordering.Int.reverse)
                val random = new Random(1234567890)

                for (i <- 0 until 200) {
                    val value = random.nextInt(3)
                    if (value == 0) {
                        if (minPriorityQueue.size < count_of_entries) {
                            val rank = random.nextInt(65535)
                            minPriorityQueue.enqueue(rank)
                            push(dut, rank, minPriorityQueue.head)
                            nop(dut, minPriorityQueue.head)
                        }
                    } else if (value == 1){
                        if (minPriorityQueue.nonEmpty) {
                            pop(dut, minPriorityQueue.dequeue())
                        } else {
                            pop(dut, 65535)
                        }
                        if (minPriorityQueue.nonEmpty) {
                            nop(dut, minPriorityQueue.head)
                        } else {
                            nop(dut, 65535)
                        }
                    } else {
                        val rank = random.nextInt(65535)
                        minPriorityQueue.enqueue(rank)
                        minPriorityQueue.dequeue()
                        if (minPriorityQueue.nonEmpty) {
                            replace(dut, rank, minPriorityQueue.head)
                        } else {
                            replace(dut, rank, 65535)
                        }
                        if (minPriorityQueue.nonEmpty) {
                            nop(dut, minPriorityQueue.head)
                        } else {
                            nop(dut, 65535)
                        }
                    }
                }
            }
    }
}