package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._
import fpga.pheap.Param._


class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val rpus = Seq.tabulate(count_of_levels + 1) { level =>
        val level_module = Module(new RPU(level))
        level_module
    }

    // 直接在rpus(1)中将结果读出
    io.entry_out := rpus(1).io.left_node_out.entry

    val token = TokenNode.default(1)
    token.op := io.op_in
    rpus(1).io.token_in := token

    rpus.last.io.lc_node_in := DontCare
    rpus.last.io.rc_node_in := DontCare

    // rpus(0)在这里没有作用
    rpus(0).io.token_in      := DontCare
    rpus(0).io.lc_node_in    := DontCare
    rpus(0).io.rc_node_in    := DontCare

    for (i <- 1 until count_of_levels) {
        rpus(i+1).io.token_in     := rpus(i).io.token_out
        rpus(i).io.lc_node_in     := rpus(i+1).io.left_node_out
        rpus(i).io.rc_node_in     := rpus(i+1).io.right_node_out
    }
}
