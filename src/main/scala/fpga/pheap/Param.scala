package fpga.pheap

import chisel3._
import chisel3.util._
import scala.math._
import fpga.Const._

object Param {

    def get_level(): Int = log2Ceil(count_of_entries + 1)

    val count_of_levels = get_level()

    def capacity_width(level: Int) = count_of_levels + 1 - level
    
    def get_capacity(level: Int) = -1.S(capacity_width(level).W).asUInt

    def position_width(level: Int) = if(level <= 1) 1 else level - 1

    // memory一次性读写2个节点
    def get_pair_depth(level: Int) = if(level <= 1) 1 else 1 << (level - 2)
    def get_pair_width(level: Int) = 2 * Node.get_width(level)

    def get_addr_width(level: Int) = if(level <= 2) 1 else 1 << (level - 3)

    def pos2addr(level: Int, pos: UInt): UInt = if(level <= 2) 0.U else pos >> 1.U

    def is_left(pos: UInt): Bool = pos % 2.U === 0.U
                
    def get_lc_pos(parent_pos: UInt): UInt = Cat(parent_pos, 0.U(1.W))

    def get_rc_pos(parent_pos: UInt): UInt = Cat(parent_pos, 1.U(1.W))

    def actual_capacity(node: Node) = node.capacity - 1.U

    def is_empty_node(node: Node) = actual_capacity(node) === 0.U

    def larger_capacity(node1: Node, node2: Node) = {
        Mux(actual_capacity(node1) > actual_capacity(node2), node1.capacity, node2.capacity)
    }

    val use_sram_param = true

}
