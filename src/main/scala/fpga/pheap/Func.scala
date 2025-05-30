package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// auxiliary functions
object Func {

    // pheap parameters
    val count_of_levels = 8  
    val mem_type = "SRAM"

    // count_of_entries -> count_of_levels
    def generate_level (node_sum : Int) : Int = {
        log2Ceil(node_sum + 1)
    }

    def capacity_width (cur_level : Int) : Int = {
        count_of_levels - cur_level + 1
    }

    def position_width (cur_level : Int) : Int = cur_level

    def data_depth (cur_level : Int) : Int = if (cur_level > 1) 1 << (cur_level - 2) else 1 

    def addr_width (cur_level : Int) : Int = {
        val depth = data_depth(cur_level)
        if (depth == 1) {
            1
        }
        else {
            log2Ceil(depth)
        }
    }
 
    // 解决初始化问题：手动生成一个默认数据
    def init_data (node : Node, cur_level : Int) : Node = {
        val no_init = node.value.asUInt === 0.U
        Mux(no_init, Node.default(cur_level), node)
    }

    // 读写单元位宽 -> 2 * Node
    def pair_width (cur_level : Int) : Int = {
        Node.getWidth(cur_level) * 2
    }

}