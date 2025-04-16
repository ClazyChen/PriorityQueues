package fpga

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import fpga.Const._
import fpga._
import fpga.shiftregister._
import scala.collection.mutable.PriorityQueue
import scala.util._
import scala.util.Random


class ShiftRegisterTest extends AnyFlatSpec with ChiselScalatestTester { 

   // 定义enqueue,dequeue,replace操作
   def enqueue(dut : PriorityQueueSR,metadata : UInt,rank : UInt,expected_rank : UInt) : Unit = {
      dut.io.op_in.push.existing.poke(true.B)
      dut.io.op_in.push.metadata.poke(metadata)
      dut.io.op_in.push.rank.poke(rank)
      dut.io.op_in.pop.poke(false.B)
      dut.clock.step()
      dut.io.entry_out.rank.expect(expected_rank)
   }

   def dequeue(dut : PriorityQueueSR,expected_rank : UInt) : Unit = {
      dut.io.op_in.pop.poke(true.B)
      dut.io.op_in.push.existing.poke(false.B)
      dut.io.op_in.push.metadata.poke(0.U(metadata_width.W))
      dut.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
      dut.clock.step()
      dut.io.entry_out.rank.expect(expected_rank)
   }

   def replace(dut : PriorityQueueSR,metadata : UInt,rank : UInt,expected_rank : UInt) : Unit = {
      // 实现replace(push-pop)操作
      dut.io.op_in.push.existing.poke(true.B)
      dut.io.op_in.push.metadata.poke(metadata)
      dut.io.op_in.push.rank.poke(rank)
      dut.io.op_in.pop.poke(true.B)
      dut.clock.step()
      dut.io.entry_out.rank.expect(expected_rank)
   }

   "PriorityQueue" should "enqueue and dequeue properly" in {
      test(new PriorityQueueSR).withAnnotations(Seq(WriteVcdAnnotation)) { dut => 

         // 先手动验证一下enqueue和dequeue操作
         enqueue(dut,1.U(metadata_width.W),1.U(rank_width.W),1.U(rank_width.W))
         enqueue(dut,2.U(metadata_width.W),2.U(rank_width.W),1.U(rank_width.W))
         enqueue(dut,3.U(metadata_width.W),3.U(rank_width.W),1.U(rank_width.W))
         dequeue(dut,2.U(rank_width.W))
         dequeue(dut,3.U(rank_width.W))
         dequeue(dut,-1.S(rank_width.W).asUInt)
         dequeue(dut,-1.S(rank_width.W).asUInt)

         // 手动测试一下replace函数
         enqueue(dut,1.U(metadata_width.W),1.U(rank_width.W),1.U(rank_width.W))
         replace(dut,3.U(metadata_width.W),3.U(rank_width.W),3.U(rank_width.W))
         enqueue(dut,4.U(metadata_width.W),4.U(rank_width.W),3.U(rank_width.W))
         replace(dut,5.U(metadata_width.W),5.U(rank_width.W),4.U(rank_width.W))
         // 如果新push进来的entry的rank最小，则立即被pop出去
         replace(dut,1.U(metadata_width.W),1.U(rank_width.W),4.U(rank_width.W))
         dequeue(dut,5.U(rank_width.W))
         dequeue(dut,-1.S(rank_width.W).asUInt)

         // 进行随机验证
         val random_seed = 2025 // 初始化随机种子
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
               println("minvalue:" + min_value)
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