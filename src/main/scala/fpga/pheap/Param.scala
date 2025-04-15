package fpga.pheap

import chisel3._
import chisel3.util._
import scala.math._
import fpga.Const._

object Param {

    def get_level(): Int = log2Ceil(count_of_entries + 1)

    val count_of_levels = get_level

    def get_capacity(level: Int) = ((1 << (count_of_levels + 1 - level)) - 1)

    def capacity_width(level: Int) = log2Ceil(get_capacity(level))

    def position_width(level: Int) = if(level <= 1) 1 else level - 1

    def get_data_depth(level: Int) = 1 << position_width(level)

    // 这里idx是在整个堆中的索引,pos是当前level中的索引
    def idx2pos(level: Int, idx: Int) = if(level <= 1) 1 else idx(level - 2, 0)
        
    def pos2idx(level: Int, pos: Int) = if(level <= 1) 0 else (1 << (level - 1)) + pos
        
    def get_lc_pos(level: Int, pos: Int): Int = {
        val parent_idx = pos2idx(level, pos)
        val lc_idx = 2 * parent_idx
        idx2pos(lc_idx)
    }
    def get_rc_pos(level: Int, pos: Int): Int = {
        val parent_idx = pos2idx(level, pos)
        val rc_idx = 2 * parent_idx + 1
        idx2pos(rc_idx)
    }

    val use_sram_param = true

}