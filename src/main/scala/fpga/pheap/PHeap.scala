package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._
import fpga.pheap.Param._


class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val rpus = Seq.tabulate(count_of_levels) { level =>
        val level_module = Module(new RPU(level))
        level_module.init_memory()
        level_module
    }

    io.entry_out := rpus(1).io.left_node_out
    
    rpus.head.io.token_in := TokenNode.init(0)

    when(io.op_in.pop && io.op_in.push.existing && rpus(1).io.left_node_out.entry < io.op_in.push) {

    } .otherwise {
        rpus(1).io.token_in := TokenNode.init(1,io.op_in.push, io.op_in)
    }

    rpus.last.io.lc_node_in := DontCare
    rpus.last.io.rc_node_in := DontCare

    for (i <- 0 until count_of_levels) {
        rpus(i) ~> rpus(i + 1)
    }

    // if(debug) {
    //      io.dbgPort.foreach { dbgPort =>
    //          dbgPort := blocks.map(_.io.entry_out)
    //      }
    //  }
}
