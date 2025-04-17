package fpga.pheap

import fpga.Const._
import chisel3.util._

// the constants for the pheap

object Const {
    // the number of levels in pheap
    val pheap_level = log2Ceil(count_of_entries + 1)

    // the type of memory used in pheap
    val pheap_mem = "SRAM" 

    def capacity_width(level : Int) : Int = {
        if (level <= pheap_level) {
            pheap_level - level + 1
        } else {
            1 // UB
        }
    }

    def position_width(level : Int) : Int = level

    def data_depth(level : Int) : Int = if (level > 1) level - 1 else 1

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