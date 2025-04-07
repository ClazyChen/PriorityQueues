package fpga.sa

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// 定义脉动阵列Block结构
class Block extends Module {
    val io = IO(new Bundle {
       val op_in = Input(new Operator)          
       val op_out = Output(new Operator)         
       val entry_in = Input(new Entry) 
       val entry_out = Output(new Entry)    
    })

    val entry = RegInit(Entry.default)
    val op = RegInit(Operator.nop)

    io.entry_out := entry_out
    io.op_out := op

    // use this function to generate what should be passed and what should be updated
    def cal_forward_and_update(Entry : current_entry, Entry : input_entry) -> (Entry, Entry) {
        val update_here = current_entry.rank < input_entry.rank
        return Mux(update_here,(input_entry, current_entry),(current_entry, input_entry))
    }

    // decision logic
    when (io.op_in.pop) {
        entry := entry_in
        op.push := Entry.default
        op.pop := true.B
    }.otherwise {
        val (forward_entry, update_entry) = cal_forward_and_update(entry, io.op_in.push)
        entry := update_entry
        op.push := forward_entry
        op.pop := false.B
    }


    def -> (next:Block) => {
        next.io.op_in <> this.io.op_out
        this.io.entry_in <> next.io.entry_out
    }

}                                                                            