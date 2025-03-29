package fpga.sr

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._


class SystolicBlock extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val op_out = Output(new Operator)

        val next_entry_in = Input(new Entry)

        val entry_out = Output(new Entry)
        
    })

    val entry = RegInit(Entry.default)
    val temp = RegInit(Entry.default)

    val op = RegInit(Operator.default)

    io.entry_out := entry
    io.op_out.push := temp

    val cmp = io.op_in.push < entry

    when(io.op_in.pop) {
        // 假设没有replace
        entry := Mux(temp < io.next_entry_in, temp, io.next_entry_in)
        temp := Entry.default
        op.push := Entry.default
        op.pop := true.B
    } .otherwise {
        when(cmp) {
            temp := entry
            op.push := entry
            entry := io.op_in.push
        } .otherwise {
            temp := io.op_in.push
            op.push := io.op_in.push
        }
        op.pop := false.B
    }

    io.op_out := op

    def ->(next: SystolicBlock) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
    }
}