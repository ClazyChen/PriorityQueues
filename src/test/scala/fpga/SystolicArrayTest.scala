package fpga

import chisel3._
import chisel3.util._
import chiseltest._
import scala.util._
import org.scalatest.flatspec.AnyFlatSpec
import fpga.Const._
import fpga._
import fpga.systolicarray._

class SystolicArrayTest extends AnyFlatSpec with ChiselScalatestTester {
    
    // 定义入队操作
    def enqueue(dut : PriorityQueueSA,metadata : UInt,rank : UInt,expected_rank : UInt) : Unit = {
        dut.io.op_in.pop.poke(false.B)
        dut.io.op_in.push.existing.poke(true.B)
        dut.io.op_in.push.metadata.poke(metadata)
        dut.io.op_in.push.rank.poke(rank)
        dut.clock.step(1)
        dut.io.entry_out.rank.expect(expected_rank)
    }
    // 定义出队操作
    def dequeue(dut : PriorityQueueSA,expected_rank : UInt) : Unit = {
        dut.io.op_in.pop.poke(true.B)
        dut.io.op_in.push.existing.poke(false.B)
        dut.clock.step(1)
        dut.io.entry_out.rank.expect(expected_rank)
    }

    // 定义replace操作
    def replace(dut : PriorityQueueSA,metadata : UInt,rank : UInt,expected_rank : UInt) : Unit = {
        dut.io.op_in.pop.poke(true.B)
        dut.io.op_in.push.existing.poke(true.B)
        dut.io.op_in.push.metadata.poke(metadata)
        dut.io.op_in.push.rank.poke(rank)
        dut.clock.step(1)
        dut.io.entry_out.rank.expect(expected_rank)
    }


    "PriorityQueue" should "enqueue and dequeue properly" in {
      test(new PriorityQueueSA).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        // 还是先进行手动验证，用手动模拟结果和实际结果对比
        dut.io.op_in.push.existing.poke(false.B)
        dut.io.op_in.pop.poke(false.B)

        // 验证初态
        dequeue(dut,-1.S(rank_width.W).asUInt)
        dequeue(dut,-1.S(rank_width.W).asUInt)

        // 验证入队操作
        enqueue(dut,5.U(metadata_width.W),5.U(rank_width.W),5.U(rank_width.W))
        enqueue(dut,3.U(metadata_width.W),3.U(rank_width.W),3.U(rank_width.W))
        enqueue(dut,2.U(metadata_width.W),2.U(rank_width.W),2.U(rank_width.W))
        enqueue(dut,8.U(metadata_width.W),8.U(rank_width.W),2.U(rank_width.W))

        // 验证replace操作
        replace(dut,1.U(metadata_width.W),1.U(rank_width.W),2.U(rank_width.W)) // 最小的元素刚入队就被pop出
        replace(dut,4.U(metadata_width.W),4.U(rank_width.W),3.U(rank_width.W))
        replace(dut,7.U(metadata_width.W),7.U(rank_width.W),4.U(rank_width.W))

        // 验证出队操作
        dequeue(dut,5.U(rank_width.W))
        dequeue(dut,7.U(rank_width.W))
        dequeue(dut,8.U(rank_width.W))
        dequeue(dut,-1.S(rank_width.W).asUInt)

        // 进行随机测试
        val random_seed = 2026 // 设置随机种子
        Random.setSeed(random_seed)
        val random_array = Array.fill(100)(Random.nextInt(65535))
        val sorted_array = random_array.sorted
        var min_value = 65535

        for (epoch <- 0 until 1) { // 测试轮数可以自行修改
          min_value = 65535;
          // 验证enqueue
          for (i <- 0 until 100) {
              println("randomvalue:" + random_array(i))
              if(min_value > random_array(i)) {
                println("minvalue:" + min_value + "> randomvalue:" + random_array(i))
                min_value = random_array(i)
              }
              println("minvalue update:" + min_value)
              enqueue(dut,0.U(metadata_width.W),random_array(i).U(rank_width.W),min_value.U(rank_width.W))
              println("enqueue success!")
          }
          // 验证dequeue
          for (i <- 0 until count_of_entries - 1) {
              dequeue(dut,sorted_array(i + 1).U(rank_width.W))
              println("dequeue success!")
          }
          // 剩下的部分都为空
          for (i <- 0 until 10) {
              dequeue(dut,-1.S(rank_width.W).asUInt)
              println("empty queue!")
          }
          // 验证replace(push-pop)
          enqueue(dut,0.U(metadata_width.W),1.U(rank_width.W),1.U(rank_width.W)) // 先在空队列里插入一个元素
          for(i <- 0 until 30) {
              replace(dut,0.U(metadata_width.W),(i + 2).U(rank_width.W),(i + 2).U(rank_width.W))
          }

        }
      }
    }
}