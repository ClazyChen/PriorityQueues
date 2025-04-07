package fpga.sa

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// systolic array block
class Block extends Module {
    val io = IO(new Bundle {
       val op_in = Input(new Operator)          
       val op_out = Output(new Operator)         
       val entry_in = Input(new Entry) 
       val entry_out = Output(new Entry)    
    })

    val entry = RegInit(Entry.default)
    val op = RegInit(Operator.nop) // pipelined register

    io.entry_out := entry
    io.op_out := op

    // use this function to generate what should be passed and what should be updated
    def cal_forward_and_update(current_entry : Entry, push_entry : Entry) : (Entry, Entry) =  {
        val dont_update_here = current_entry < push_entry
        (Mux(dont_update_here,push_entry,current_entry),Mux(dont_update_here,current_entry,push_entry)) // cost depends on bit width
    }

    val (forward_entry, update_entry) = cal_forward_and_update(entry, io.op_in.push)

    // decision logic supports for push and pop only
    when(io.op_in.pop) {
        entry := io.entry_in
    }.otherwise {
        entry := update_entry
    }

    op.pop := io.op_in.pop
    op.push := forward_entry

    io.entry_out := update_entry // use entry may cause time-sequence problems

    def ~> (next : Block) = {
        next.io.op_in <> this.io.op_out
        this.io.entry_in <> next.io.entry_out
    }

}                                                                            