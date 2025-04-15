package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._



class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val rpus = VecInit(Seq.tabulate(count_of_levels) { level =>
        val level_module = Module(new RPU(level))
        level_module.init_memory()
        level_module
    })

    io.entry_out := read(1, rpus.head.mem_out, 0)
    
    rpus.head.token_in := TokenNode.default(0)
    rpus(1).token_in := TokenNode.init(1, io.entry_in, io.op_in)

    rpus.last.mem_in := DontCare

    for (i <- 0 until count_of_levels) {
        rpus(i) ~> rpus(i + 1)
    }
}
