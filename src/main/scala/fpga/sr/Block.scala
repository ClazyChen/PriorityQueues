package fpga.sr

import chisel3._
import fpga.Const._
import fpga._

class Block extends Module{
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val prev_entry_in = Input(new Entry)
        val next_entry_in = Input(new Entry)
        val prev_cmp_in = Input(Bool())
        val next_cmp_in = Input(Bool())
        val entry_out = Output(new Entry)
        val cmp_out = Output(Bool())
    })

    val entry = RegInit(Entry.default)
    val cmp = Wire(Bool()) // 比较结果
    cmp := io.op_in.push < entry
    io.cmp_out := cmp
    io.entry_out := entry

    //    when(io.op_in.pop && !io.op_in.push.existing) {
    //        entry := io.next_entry_in
    //    }.elsewhen(!io.op_in.pop && io.op_in.push.existing) {
    //        when (cmp) {
    //            entry := Mux(io.prev_cmp_in, io.prev_entry_in, io.op_in.push)
    //        }
    //    }.elsewhen(io.op_in.pop && io.op_in.push.existing) {
    //        when (!cmp) {
    //            entry := Mux(io.next_cmp_in, io.op_in.push, io.next_entry_in)
    //        }
    //    }

    when(io.op_in.pop) {
        when(!cmp) {
            entry := Mux(io.next_cmp_in, io.op_in.push, io.next_entry_in)
        }
    }.otherwise {
        when(cmp) {
            entry := Mux(io.prev_cmp_in, io.prev_entry_in, io.op_in.push)
        }
    }
}