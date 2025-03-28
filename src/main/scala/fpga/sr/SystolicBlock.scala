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

    io.entry_out := entry
    io.op_out.push := temp

    val cmp = io.op_in.push < entry

    when(io.op_in.pop) {
        // 假设没有replace
        // entry := io.next_entry_in
        entry := Mux(temp < io.next_entry_in, temp, io.next_entry_in)
        io.op_out.pop := true.B
        io.op_out.push := Entry.default
    } .otherwise {
        when(cmp) {
            temp := entry
            entry := io.op_in.push
            io.op_out.pop := false.B
            io.op_out.push := temp
        } .otherwise {
            temp := io.op_in.push
            io.op_out.pop := false.B
            io.op_out.push := temp
        }
    }

    // io.op_out := RegNext(io.op_in, init = Operator.default)

    def ->(next: SystolicBlock) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
    }
}