package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._
import fpga.pheap.Param.Const._



class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val mems = (0 to pheap_levels).map { level =>
        val data_depth = if(level == 0) 1 else 1 << (level - 1)
        Module(new Memory(data_depth, rank_width, true))
    }
    val tokens = RegInit(Seq.fill(pheap_levels + 1)(new TokenNode))

    if(io.op_in.pop) {

    } .otherwise {

    }

    for (i <- 0 to levels) {
        val pos = tokens(i).position
        val mem = mems(i)
        val addr = pos(i, 0)
        mem.io.addr := addr
        mem.io.en   := true.B
        mem.io.wen  := false.B  // by default a read
        val storedPacked = mem.io.data_out

        val nextToken = Wire(new TokenBundle(totalNodes, params.priorityWidth))
        nextToken.op       := tokens(i).op       // In a real design this may change after comparisons.
        nextToken.entry    := tokens(i).entry    // Placeholder: no swap is performed.
        nextToken.position := tokens(i).position << 1
        tokens(i + 1) := nextToken
    }

    when (tokens(levels - 1).op === 2.U || tokens(levels - 1).op === 3.U) {
        io.entry_out := tokens(levels - 1).entry
    } .otherwise {
        // If no pop, provide a default (invalid) entry.
        io.entry_out := Entry.default
    }


    when (io.op_in.push =/= Entry.default || io.op_in.pop) {
        // Initialize the root token.
        tokens(0).op       := Mux(io.op_in.pop, Mux(io.op_in.push =/= Entry.default, 3.U, 2.U), 1.U)
        tokens(0).entry    := io.op_in.push
        tokens(0).position := 1.U  // root position is 1
    }
}
