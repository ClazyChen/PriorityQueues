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
        // val temp_out = Output(new Entry)
    })

    val entry = RegInit(Entry.default)
    val temp = RegInit(Entry.default)

    val op = Wire(new Operator)

    io.entry_out := entry
    io.op_out.push := temp
    // io.temp_out := temp

    val cmp = io.op_in.push < entry

    def min(a: Entry, b: Entry, c: Entry): Entry = {
        val minAB = Mux(a < b, a, b)
        Mux(minAB < c, minAB, c)
    }

    // TODO 比较器太多了,应该可以简化
    when(io.op_in.pop) {
        // 如果push小于temp和next,push放置在当前block,不需要从后面的块pop
        when(io.op_in.push < temp && io.op_in.push < io.next_entry_in) {
            entry := Mux(cmp, entry, io.op_in.push)
            op := Operator.default
        // 否则,从后面的块获取entry,push需要向后传递
        } .otherwise {
            entry := min(io.op_in.push, temp, io.next_entry_in)
            op.push := io.op_in.push
            op.pop := true.B
        }
    } .otherwise {
        op.push := Mux(cmp, entry, io.op_in.push)
        entry := Mux(cmp, io.op_in.push, entry)
        op.pop := false.B
    }
    
    temp := op.push
    io.op_out := op

    def ->(next: Block) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
    }
}