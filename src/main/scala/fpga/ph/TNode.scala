package fpga.ph

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.ph.Op._


// node in token array (for pipeline)
class TNode(val level : UInt) extends Bundle {

    val op = new Operator // equals to field operation + value

    val position = UInt(count_of_levels.W)
}

