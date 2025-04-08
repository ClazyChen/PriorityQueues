package fpga.ph

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.ph.BNode

class BArray (mem_types : Seq[String]) extends Module {

    val depth = (1 << count_of_levels) - 1
    val data_uint = BNode.asUInt
    
    // generate binary array memory blocks
    val binary_array_mem_blocks = Seq.tabulate(count_of_levels) { i =>
        mem_types(i) match {
            case "FFMem" => Module(new FFMem(depth, data_uint_width.W))
            case "SRAM"  => Module(new SRAM(depth, data_uint_width.W))
            case unknown => throw new IllegalArgumentException(s"未知存储器类型在第 ${i} 层: $unknown")
        }
    }

    // link diferent blocks 
    for (i <- 0 until count_of_levels - 1) {
        // declare two adjacent states
        val current_block = binary_array_mem_blocks(i)
        val next_block = binary_array_mem_blocks(i + 1)

        next_block.io.w.addr := current_block.io.r.addr
        next_block.io.w.data := current_block.io.r.data
        current_block.io.w.addr := next_block.io.r.addr
        current_block.io.w.data := next_block.io.r.data
    }
}
