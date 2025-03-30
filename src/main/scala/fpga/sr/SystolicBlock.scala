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
        val temp_out = Output(new Entry)
    })

    val entry = RegInit(Entry.default)
    val temp = RegInit(Entry.default)

    val op = Wire(new Operator)

    io.entry_out := entry
    io.op_out.push := temp
    io.temp_out := temp

    val cmp = io.op_in.push < entry

    def min(a: Entry, b: Entry, c: Entry): Entry = {
        val minAB = Mux(a < b, a, b)
        Mux(minAB < c, minAB, c)
    }

    when(io.op_in.pop) {
        when(io.op_in.push.existing) {
            entry := Mux(cmp, entry, min(io.op_in.push, temp, io.next_entry_in))
        } .otherwise {
            entry := Mux(temp < io.next_entry_in, temp, io.next_entry_in)
        }
        op.push := Mux(io.op_in.push.existing && !cmp, io.op_in.push, Entry.default)
        op.pop := Mux(cmp, false.B, true.B)
    } .otherwise {
        op.push := Mux(cmp, entry, io.op_in.push)
        entry := Mux(cmp, io.op_in.push, entry)
        op.pop := false.B
    }
    
    // op.pop := io.op_in.pop && io.op_in.push.existing && !cmp || io.op_in.pop && !io.op_in.push.existing
    temp := op.push
    io.op_out := op

    def ->(next: SystolicBlock) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
    }
}