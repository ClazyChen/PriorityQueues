package fpga.ph

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.ph.TNode

// consider using ffmem to implement token array
class TArray extends Module {

    // generate blocks
    // data_depth is equivalent to entry nums
    // need only a single mem ? 
    val token_array_mem = Module(new FFMem((1 << count_of_levels) - 1,TNode.asUInt.W))

    
}