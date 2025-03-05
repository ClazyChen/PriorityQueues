import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random

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
                        dut.io.read.poke(false.B)
                        dut.io.write.poke(true.B)
                        dut.io.newEntry.priority.poke(randomArray(i))
                        dut.io.newEntry.metadata.poke(0)
                        dut.clock.step()
                        if (minValue > randomArray(i)) {
                            minValue = randomArray(i)
                        }
                        dut.io.highestEntry.priority.expect(minValue.U)
                    }

                    // 验证队列中是否剩下20个优先级最高元素
                    for (i <- 0 until 19) {
                        dut.io.read.poke(true.B)
                        dut.io.write.poke(false.B)
                        dut.clock.step()
                        dut.io.highestEntry.priority.expect(sortedArray(i + 1).U)
                    }

                    // 剩下应该都是空
                    for (i <- 0 until 20) {
                        dut.io.read.poke(true.B)
                        dut.io.write.poke(false.B)
                        dut.clock.step()
                        dut.io.highestEntry.priority.expect(65535.U)
                    }
                }

            }
    }

//    "ShiftRegister" should "correct" in {
//        test(new ShiftRegister(16, 0, 10))
//            .withAnnotations(Seq(WriteVcdAnnotation))  { dut =>
//
//                // 插入10
//            dut.io.read.poke(0.U)
//            dut.io.write.poke(1.U)
//            dut.io.newEntry.poke(10.U)
//            dut.clock.step()
//            dut.io.highestEntry.expect(10.U)
//
//                // 插入30
//            dut.io.read.poke(0.U)
//            dut.io.write.poke(1.U)
//            dut.io.newEntry.poke(30.U)
//            dut.clock.step()
//            dut.io.highestEntry.expect(10.U)
//
//                // 弹出10
//            dut.io.read.poke(1.U)
//            dut.io.write.poke(0.U)
//            dut.clock.step()
//            dut.io.highestEntry.expect(30.U)
//
//                // 插入20
//            dut.io.read.poke(0.U)
//            dut.io.write.poke(1.U)
//            dut.io.newEntry.poke(20.U)
//            dut.clock.step()
//            dut.io.highestEntry.expect(20.U)
//
//            for (i <- 0 until 15) {
//                // 生成一个 8 位的随机无符号整数
//                dut.io.read.poke(false.B)
//                dut.io.write.poke(true.B)
//                dut.io.newEntry.poke((20-i).U)
//                dut.clock.step()
//                dut.io.highestEntry.expect((20-i).U)
//            }
//        }
//    }
}