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
        dut.clock.step(150)
        dut.io.entry_out.rank.expect(expected_rank)
    }
    // 定义出队操作
    def dequeue(dut : PriorityQueueSA,expected_rank : UInt) : Unit = {
        dut.io.op_in.pop.poke(true.B)
        dut.io.op_in.push.existing.poke(false.B)
        dut.io.op_in.push.metadata.poke(0.U)
        dut.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
        dut.clock.step(1)
        dut.io.entry_out.rank.expect(expected_rank)
    }

    "PriorityQueue" should "enqueue and dequeue properly" in {
      test(new PriorityQueueSA).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        // 先进行手动验证
        // 验证初态
        dequeue(dut,-1.S(rank_width.W).asUInt)
        dequeue(dut,-1.S(rank_width.W).asUInt)

        // 验证入队操作
        enqueue(dut,5.U(metadata_width.W),5.U(rank_width.W),5.U(rank_width.W))
        enqueue(dut,3.U(metadata_width.W),3.U(rank_width.W),3.U(rank_width.W))
        enqueue(dut,2.U(metadata_width.W),2.U(rank_width.W),2.U(rank_width.W))
        enqueue(dut,8.U(metadata_width.W),8.U(rank_width.W),2.U(rank_width.W))
        enqueue(dut,1.U(metadata_width.W),1.U(rank_width.W),1.U(rank_width.W))

        // 验证出队操作
        dequeue(dut,2.U(rank_width.W))



      }
    }
}