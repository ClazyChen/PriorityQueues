package fpga.sr

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// a block in the shift register
class Block extends Module {
    val io = IO(new Bundle {
        val op_in = Input(new Operator)
        val prev_entry_in = Input(new Entry)
        val next_entry_in = Input(new Entry)
        val prev_cmp_in = Input(Bool())
        val next_cmp_in = Input(Bool())
        val cmp_out = Output(Bool())
        val entry_out = Output(new Entry)
    })

    // the entry stored in the block
    val entry = RegInit(Entry.default)
    io.entry_out := entry

    // the comparison result
    val cmp = io.op_in.push < entry
    io.cmp_out := cmp
    
    // update the entry
    when (io.op_in.pop) {
        // for pop and replace operation
        // 1. shift-left : e'(i) = e(i+1) , when cmp = 0 and cmp(i+1) = 0
        // 2. replace    : e'(i) = push   , when cmp = 0 and cmp(i+1) = 1
        // 3. no change  : e'(i) = e(i)  , otherwise
        when (!cmp) {
            entry := Mux(io.next_cmp_in, io.op_in.push, io.next_entry_in)
        }
    } .otherwise {
        // for push operation
        // 1. shift-right : e'(i) = e(i-1) , when cmp = 1 and cmp(i-1) = 1
        // 2. replace     : e'(i) = push   , when cmp = 1 and cmp(i-1) = 0
        // 3. no change   : e'(i) = e(i)  , otherwise
        when (cmp) {
            entry := Mux(io.prev_cmp_in, io.prev_entry_in, io.op_in.push)
        }
    }

    // connect blocks, this ~> next
    def ~>(next: Block) = {
        next.io.prev_entry_in := this.io.entry_out
        next.io.prev_cmp_in := this.io.cmp_out
        this.io.next_entry_in := next.io.entry_out
        this.io.next_cmp_in := next.io.cmp_out
    }
}
