// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
package ShiftRegisterPQ

import chisel3._;
import chisel3.util._;
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec;

class ShiftRegisterPriorityQueueTest extends AnyFlatSpec with ChiselScalatestTester {

    def dequeue (c : PriorityQueueBlock) : UInt = {
        c.io.read.poke(true.B);
        c.io.write.poke(false.B);
        c.clock.step(1);
        ()
    }

    def enqueue (c : PriorityQueueBlock,rank : Int) : UInt = {
        c.io.write.poke(true.B);
        c.io.read.poke(false.B);
        c.io.new_entry.rank.poke(rank.U);
        c.clock.step(1);
        ()
    }

    "PriorityQueueBlock" should "enqueue and dequeue entries properly" in {

        test(new PriorityQueueBlock(0,4,4)) { c => 

        c.io.output_entry.expect(0xf.U);

        // 入队操作
        // 初始化信号
        // c.io.read.poke(false.B)
        // c.io.write.poke(true.B)
        // c.io.new_entry.valid.poke(true.B)
        // c.io.new_entry.bits.address.poke(1.U)
        // c.io.new_entry.bits.priority.poke(5.U)

        // // 推进时钟
        // c.clock.step(1)

        // // 检查队列状态
        // c.io.new_entry.valid.poke(false.B)
        // c.io.write.poke(false.B)
        // c.clock.step(1)

        // // 查看队首条目
        // c.io.read.poke(true.B)
        // c.clock.step(1)
        // c.io.output_entry.address.expect(1.U)
        // c.io.output_entry.priority.expect(5.U)

        // 初始态
        


        }
    }

}