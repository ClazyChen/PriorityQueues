package fpga

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import fpga.Const._
import fpga._
import fpga.shiftregister._
import scala.collection.mutable.PriorityQueue

class ShiftRegisterTest extends AnyFlatSpec with ChiselScalatestTester {    

     "PriorityQueue" should "enqueue and dequeue properly" in {
        test(new PriorityQueue) { dut => 
            
        }
     }
}