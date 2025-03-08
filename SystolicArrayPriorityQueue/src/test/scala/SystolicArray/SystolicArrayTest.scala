package SystolicArrayPQ

import chisel3._;
import chisel3.util._;
import chiseltest._;
import org.scalatest.flatspec.AnyFlatSpec;

class SystolicArrayTest extends AnyFlatSpec with ChiselScalatestTester {

    "PriorityQueueBlock" should "enqueue and dequeue properly" in {

        test(new PriorityQueueBlock(0,4,4)) { c =>
            

        }
    }
}