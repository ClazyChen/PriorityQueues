package fpga.ph

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// four types of operation in token array nodes
// to be modified or removed , may be Operator is enough ? 
class PhOp extends Bundle {

    val enq = Bool()
    
    val deq = Bool()

    val edq = Bool()

    val nop = Bool()

}