package fpga.pheap

import chisel3._
import chisel3.util._
import scala.math._
import fpga.Const._

object Param {

    def get_level(): Int = log2Ceil(count_of_entries + 1)

    val count_of_levels = get_level()

    def get_capacity(level: Int): UInt = ((1 << (count_of_levels + 1 - level)) - 1).U

    def capacity_width(level: Int) = count_of_levels + 1 - level

    def position_width(level: Int) = if(level <= 1) 1 else level - 1

    // memory一次性读写2个节点
    def get_pair_depth(level: Int) = if(level <= 1) 1 else 1 << (level - 2)
    def get_pair_width(level: Int) = 2 * Node.getWidth(level)

    def get_addr_width(level: Int) = if(level <= 2) 1 else 1 << (level - 3)

    def pos2addr(level: Int, pos: UInt): UInt = if(level <= 2) 0.U else pos >> 1.U

    def is_left(pos: UInt): Bool = pos % 2.U === 0.U
                
    def get_lc_pos(parent_pos: UInt): UInt = 2.U * parent_pos

    def get_rc_pos(parent_pos: UInt): UInt = 2.U * parent_pos + 1.U

    val use_sram_param = true

}
