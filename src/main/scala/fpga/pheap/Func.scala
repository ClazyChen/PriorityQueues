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

    // number of nodes
    def data_depth (cur_level : Int) : Int = 1 << (cur_level - 1)

    def addr_width (cur_level : Int) : Int = {
        if (cur_level == 1) {
            1
        }
        else {
            log2Ceil(data_depth(cur_level))
        }
    }

    // global_index to local_index
    def g2l (global_index : UInt, cur_level : Int) : UInt = {
        global_index - (1.U << (cur_level - 1))
    }
 
    // 解决初始化问题：如果sram尚未保存数据，手动生成一个默认数据
    def init_data (node : Node, level : Int) : Node = {
        Mux(node.value.asUInt === 0.U, Node.default(level), node)
    }

}