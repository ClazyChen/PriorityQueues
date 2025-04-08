package fpga.ph

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.ph.BNode
import fpga.ph.TNode
import fpga.ph.BArray
import fpga.ph.TArray

// top-module : P-Heap
class PHeap (mem_types : Seq[String]) extends Module {
    
    // io link to outer module
    val io = IO(new PQIO)

    // generate two parts
    val b_array = Module(new BArray(mem_types))
    val t_array = Module(new TArray)

}