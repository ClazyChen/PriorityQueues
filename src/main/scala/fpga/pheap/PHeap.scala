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
        Module(new RPU(level))
    }

    // 直接在rpus(1)中将结果读出
    io.entry_out := rpus(1).io.pair_out.first.entry

    when(io.op_in.pop && io.op_in.push.existing && io.op_in.push > io.entry_out) {
        rpus(1).io.token_in := TokenNode.default(1)
    } .otherwise {
        val token = TokenNode.default(1)
        token.op := io.op_in
        rpus(1).io.token_in := token
    }

    rpus.last.io.pair_in := DontCare

    // rpus(0)在这里没有作用
    rpus(0).io.token_in      := DontCare
    rpus(0).io.pair_in       := DontCare

    for (i <- 1 until count_of_levels) {
        rpus(i+1).io.token_in     := rpus(i).io.token_out
        rpus(i).io.pair_in        := rpus(i+1).io.pair_out
    }
}
