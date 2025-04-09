package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._
import fpga.pheap.Param._



class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val pheap_levels = VecInit(Seq.tabulate(count_of_levels) { level =>
        Module(new PHeapLevel(level))
    })

    // TODO 引入哨兵会增加一级延迟,需要修改
    io.entry_out := read(pheap_levels.head.mem_out, 0)
    
    val token = TokenNode.init(io.entry_in, io.op_in)
    pheap_levels.head.token_in := token

    // TODO 最后一个level的子level怎么初始化?
    // pheap_levels.last.mem_in := ?

    for (i <- 0 until count_of_levels) {
        pheap_levels(i) ~> pheap_levels(i + 1)
    }
}
