package fpga.pheap

import chisel3._
import chisel3.util._
import scala.math._
import fpga.Const._

object Param {

    def get_level(): Int = log2Ceil(count_of_entries + 1)

    val count_of_levels = get_level()

    def get_capacity(level: Int): UInt = ((1 << (count_of_levels + 1 - level)) - 1).U

    def capacity_width(level: Int) = log2Ceil(((1 << (count_of_levels + 1 - level)) - 1))

    def position_width(level: Int) = if(level <= 1) 1 else level - 1

    def get_data_depth(level: Int) = 1 << position_width(level)
                
    def get_lc_pos(level: Int, pos: UInt): UInt = 2.U * pos

    def get_rc_pos(level: Int, pos: UInt): UInt = 2.U * pos + 1.U

    val use_sram_param = true

}
