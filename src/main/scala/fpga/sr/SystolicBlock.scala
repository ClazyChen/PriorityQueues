package fpga.sr

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class SystolicBlock extends Module {
    val io = IO(new Bundle {
        // val enq_in  = Input(Bool())
        // val deq_in  = Input(Bool())
        // val enq_out = Output(Bool())
        // val deq_out = Output(Bool())
        val op_in = Input(new Operator)
        val op_out = Output(new Operator)

        // op_in.push
        // val bubble_in  = Input(new Entry(dataWidth, rankWidth))
        val next_entry_in = Input(new Entry)

        // op_out.push
        // val bubble_out = Output(new Entry(dataWidth, rankWidth))
        val entry_out = Output(new Entry)
    })

    val entry = RegInit(Entry.default)
    val temp = RegInit(Entry.default)

    io.entry_out := entry
    io.op_out.push := temp

    val cmp = io.op_in.push < entry

    when(io.op_in.pop) {
        // 假设没有replace
        entry := io.next_entry_in
    } .otherwise {
        when(cmp) {
            entry := io.op_in.push
            temp := entry
        } .otherwise {
            temp := io.op_in.push
        }
    }

    // when(io.enq_in && !io.deq_in) {
    //     when(io.op_in.push < entry) {
    //         temp := entry
    //         entry := io.op_in.push
    //     } .otherwise {
    //         temp := io.op_in.push
    //     }
    // } .elsewhen(io.deq_in && !io.enq_in) {
    //     entry := io.next_entry_in
    // } .otherwise {
    //     temp := Entry.default(dataWidth, rankWidth)
    // }

    // io.enq_out := RegNext(io.enq_in, init = false.B)
    // io.deq_out := RegNext(io.deq_in, init = false.B)
    io.op_out := RegNext(io.op_in, init = Operator.default)

    def ->(next: SystolicBlock) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
    }
}