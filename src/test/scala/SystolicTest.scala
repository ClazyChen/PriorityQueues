import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

class SystolicTest extends  AnyFlatSpec with ChiselScalatestTester {
    "SystolicTest" should "correct" in {
        test(new Systolic(0, 16, 20))
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
                        dut.io.newEntry.priority.poke(randomArray(i))
                        dut.io.newEntry.metadata.poke(0)
                        dut.clock.step(2) // 两个周期才能执行一次操作
                        if (minValue > randomArray(i)) {
                            minValue = randomArray(i)
                        }
                        // 入队不显示优先级最高元素，出队才显示
                        // dut.io.highestEntry.priority.expect(minValue.U)
                    }

                    // 验证队列中是否剩下20个优先级最高元素
                    for (i <- 0 until 20) {
                        dut.io.dequeue.poke(true.B)
                        dut.io.enqueue.poke(false.B)
                        dut.clock.step(2)
                        dut.io.highestEntry.priority.expect(sortedArray(i).U)
                    }

                    // 剩下应该都是空
                    for (i <- 0 until 20) {
                        dut.io.dequeue.poke(true.B)
                        dut.io.enqueue.poke(false.B)
                        dut.clock.step()
                        dut.io.highestEntry.priority.expect(65535.U)
                    }
                }

            }
    }
}