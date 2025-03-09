import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.PriorityQueue

class SRTest extends  AnyFlatSpec with ChiselScalatestTester {
    "ShiftRegister" should "correct" in {
        test(new ShiftRegister(0, 16, 20))
            .withAnnotations(Seq(WriteVcdAnnotation)) {dut =>
                val randomArray = Array.fill(50)(Random.nextInt(65535))
                val sortedArray = randomArray.sorted
                var minValue = 65535

                // 重复两次
                for (_ <- 0 until 2) {
                    minValue = 65535
                    for (i <- 0 until 50) {
                        dut.io.dequeue.poke(false.B)
                        dut.io.enqueue.poke(true.B)
                        dut.io.newEntry.rank.poke(randomArray(i))
                        dut.io.newEntry.metadata.poke(0)
                        dut.clock.step()
                        if (minValue > randomArray(i)) {
                            minValue = randomArray(i)
                        }
                        dut.io.highestEntry.rank.expect(minValue.U)
                    }

                    // 验证队列中是否剩下20个优先级最高元素
                    for (i <- 0 until 19) {
                        dut.io.dequeue.poke(true.B)
                        dut.io.enqueue.poke(false.B)
                        dut.clock.step()
                        dut.io.highestEntry.rank.expect(sortedArray(i + 1).U)
                    }

                    // 剩下应该都是空
                    for (i <- 0 until 20) {
                        dut.io.dequeue.poke(true.B)
                        dut.io.enqueue.poke(false.B)
                        dut.clock.step()
                        dut.io.highestEntry.rank.expect(65535.U)
                    }

                }
            }
        test(new ShiftRegister(0, 16, 50))
            .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
                // 用自带的优先级队列测试
                val minPriorityQueue = PriorityQueue[Int]()(Ordering.Int.reverse)
                for (i <- 0 until 100) {
                    if (Random.nextInt(2) == 1) {
                        if (minPriorityQueue.size < 50) {
                            val rank = Random.nextInt(65535)
                            minPriorityQueue.enqueue(rank)
                            dut.io.dequeue.poke(false.B)
                            dut.io.enqueue.poke(true.B)
                            dut.io.newEntry.rank.poke(rank.U)
                            dut.io.newEntry.metadata.poke(0)
                            dut.clock.step()
                        }
                    } else {
                        if (minPriorityQueue.nonEmpty) {
                            dut.io.highestEntry.rank.expect(minPriorityQueue.dequeue().U)
                        } else {
                            dut.io.highestEntry.rank.expect(65535.U)
                        }
                        dut.io.dequeue.poke(true.B)
                        dut.io.enqueue.poke(false.B)
                        dut.clock.step()
                    }
                }
            }
    }
}