package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.PriorityQueueTrait

class PHeap extends Module with PriorityQueueTrait{
  val io = new PQIO

  val pheapMemInst = Module(new PHeapMem("Sram"))

}
