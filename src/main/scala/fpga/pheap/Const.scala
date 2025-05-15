package fpga.pheap

import fpga.Const._
import chisel3.util._

// the constants for the pheap

object Const {
    // the number of levels in pheap
    val pheap_level = 8

    // the type of memory used in pheap
    val pheap_mem = "FFMEM" 

    def capacity_width(level : Int) : Int = {
        val p_level = if (pheap_level > 0) pheap_level else log2Ceil(count_of_entries + 1)

        if (level <= p_level) {
            p_level - level + 1
        } else {
            1 // UB
        }
    }

    def position_width(level : Int) : Int = level

    def data_depth(level : Int) : Int = if (level > 1) 1 << (level - 2) else 1

    def addr_width(level : Int) : Int = {
        val depth = data_depth(level)
        if (depth == 1) {
            1
        } else {
            log2Ceil(depth)
        }
    }

    // 读写单位为两个node
    def rw_width(level : Int) : Int = Node.getWidth(level) * 2
}