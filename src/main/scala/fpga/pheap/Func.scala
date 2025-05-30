package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import chisel3.util._

// auxiliary functions
object Func {
    
    // pheap parameters
    val count_of_levels = 8
    val mem_type = "FFMEM" 

    def capacity_width(cur_level : Int) : Int = {
        count_of_levels - cur_level + 1
    }

    def position_width(cur_level : Int) : Int = cur_level

    def data_depth(cur_level : Int) : Int = if (cur_level > 1) 1 << (cur_level - 2) else 1

    def addr_width(cur_level : Int) : Int = {
        val depth = data_depth(cur_level)
        if (depth == 1) {
            1
        } else {
            log2Ceil(depth)
        }
    }

    // read/write data_width -> 2 * Node
    def pair_width (cur_level : Int) : Int = {
        Node.getWidth(cur_level) * 2
    }

    // initialization problem : generate a default node
    def init_data(node : Node, cur_level : Int) : Node = {
        Mux(node.entry.asUInt === 0.U, Node.default(cur_level), node)
    }
        
}