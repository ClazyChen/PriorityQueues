package SystolicArrayPQ

import chisel3._;
import chisel3.util._;
import chiseltest._;
import org.scalatest.flatspec.AnyFlatSpec;

class SystolicArrayTest extends AnyFlatSpec with ChiselScalatestTester {

    "PriorityQueueBlock" should "enqueue and dequeue properly" in {

        test(new PriorityQueueBlock(0,4,4)) { c =>
            
            // 初始态检验
            c.clock.step(1);
            c.io.read.poke(true.B);
            c.io.write.poke(false.B);
            c.io.output_entry.rank.expect(15);

            c.io.read.poke(false.B);
            c.io.write.poke(false.B);

            // dequeue操作
            c.io.read.poke(true.B);
            c.io.write.poke(true.B);
            c.io.output_entry.rank.expect(0xf.U);

            // enqueue操作
            c.io.read.poke(false.B);
            c.io.write.poke(true.B);
            c.io.new_entry.rank.poke(8.U);
            c.clock.step(1);
            c.io.output_entry.rank.expect(8.U);


        }
    }
}