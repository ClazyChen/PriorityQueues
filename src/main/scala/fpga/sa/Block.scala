package fpga.sa

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._


class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val op_out = Output(new Operator)
        val next_entry_in = Input(new Entry)
        val entry_out = Output(new Entry)
        val cmp_in = Input(Bool()) // io.op_in.push < entry
        val cmp_out = Output(Bool())
    })

    val entry = RegInit(Entry.default)
    val op = RegInit(Operator.default)

    io.entry_out := entry
    io.op_out := op
    io.cmp_out := DontCare

    val cmp = op.push < io.next_entry_in

    when(io.op_in.pop) {
        entry := Mux(cmp, op.push, io.next_entry_in)
        op := io.op_in
        io.cmp_out := cmp
    } .otherwise {
        when(io.cmp_in) {
            op.push := entry
            entry := io.op_in.push
        } .otherwise {
            op.push := io.op_in.push
        }
        op.pop := false.B
    }


    def ->(next: Block) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
        next.io.cmp_in := this.io.cmp_out
    }
}