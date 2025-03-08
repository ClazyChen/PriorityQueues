// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
package ShiftRegisterPQ

import chisel3._;
import chisel3.util._;
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec;

class ShiftRegisterPriorityQueueTest extends AnyFlatSpec with ChiselScalatestTester {

    "PriorityQueueBlock" should "enqueue and dequeue entries properly" in {

        test(new PriorityQueueBlock(0,4,4)) { c => 

        // 初始态
        c.io.output_entry.rank.expect(0xf.U);

        // enqueue测试

        // 入队8，现在队列rank为[8，15，15，15]
        c.io.read.poke(false.B)
        c.io.write.poke(true.B)
        c.io.new_entry.rank.poke(8.U);
        c.clock.step(1);
        c.io.output_entry.rank.expect(8.U);
 
        // 入队4，现在队列rank为[4,8,15,15]
        c.io.read.poke(false.B)
        c.io.write.poke(true.B)
        c.io.new_entry.rank.poke(4.U);
        c.clock.step(1);
        c.io.output_entry.rank.expect(4.U);
 
        // 入队5，现在队列rank为[4,5,8,15]
        c.io.read.poke(false.B);
        c.io.write.poke(true.B);
        c.io.new_entry.rank.poke(5.U);
        c.clock.step(1);
        c.io.output_entry.rank.expect(4.U);

        // 入队3，现在队列rank为[3,4,5,8]
        c.io.read.poke(false.B);
        c.io.write.poke(true.B);
        c.io.new_entry.rank.poke(3.U);
        c.clock.step(1);
        c.io.output_entry.rank.expect(3.U);

        // 入队6，当前队列rank为[3,4,5,6]
        c.io.read.poke(false.B);
        c.io.write.poke(true.B);
        c.io.new_entry.rank.poke(6.U);
        c.clock.step(1);
        c.io.output_entry.rank.expect(3.U);
 
        // dequeue测试
        c.io.read.poke(false.B);
        c.io.write.poke(false.B);

        // 出队3，当前队列rank为[4,5,6,15]
        c.io.read.poke(true.B);
        c.io.write.poke(false.B);
        c.clock.step(1);
        c.io.output_entry.rank.expect(4.U);
        
        // 出队4，当前队列rank为[5,6,15,15]
        c.io.read.poke(true.B);
        c.io.write.poke(false.B);
        c.clock.step(1);
        c.io.output_entry.rank.expect(5.U);

        // 出队5，当前队列rank为[6,15,15,15]
        c.io.read.poke(true.B);
        c.io.write.poke(false.B);
        c.clock.step(1);
        c.io.output_entry.rank.expect(6.U);

        // 出队6，当前队列rank为[15,15,15,15]
        c.io.read.poke(true.B);
        c.io.write.poke(false.B);
        c.clock.step(1);
        c.io.output_entry.rank.expect(0xf.U);
        
        // All test cases passed

        }
    }

}