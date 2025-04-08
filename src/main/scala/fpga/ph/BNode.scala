package fpga.ph

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// node in binary array
class BNode(val level : UInt) extends Bundle {

    val value = new Entry

    val capacity = UInt(capacity_width(level).W)

}

// calculate capacity bit-width to reduce cost
def capacity_width (cur_level : UInt) : UInt = {

    // make sure level in a proper range
    require(cur_level > 0 && cur_level <= count_of_levels, s"Level $cur_level out of range [1, ${count_of_levels}]")

    // calculate capacity_width
    (count_of_levels - cur_level + 1)

    // level  capacity  bit_width
    //  256      1          1
    //  255      3          2
    //  254      7          3
    //  253     15          4
    //  252     31          5

}