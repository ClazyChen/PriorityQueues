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
        // val temp_out = Output(new Entry)
    })

    val entry = RegInit(Entry.default)
    val temp = RegInit(Entry.default)

    val op = Wire(new Operator)

    io.entry_out := entry
    io.op_out.push := temp
    // io.temp_out := temp

    val cmp_push_temp = io.op_in.push < temp
    val cmp_push_next = io.op_in.push < io.next_entry_in
    val cmp_temp_next = temp < io.next_entry_in

    val minVal = Mux(cmp_push_temp && cmp_push_next, io.op_in.push, 
              Mux(cmp_temp_next, temp, io.next_entry_in))

    when(io.op_in.pop) {
        // 如果push小于temp和next,push放置在当前block,不需要从后面的块pop
        when(io.op_in.push < temp && io.op_in.push < io.next_entry_in) {
            entry := Mux(io.cmp_in, entry, io.op_in.push)
            op := Operator.default
            io.cmp_out := cmp_temp_next
        // 否则,从后面的块获取entry,push需要向后传递
        } .otherwise {
            entry := minVal
            op.push := io.op_in.push
            op.pop := true.B
            io.cmp_out := cmp_push_next
        }
    } .otherwise {
        when(io.cmp_in) {
            op.push := entry
            entry := io.op_in.push
            io.cmp_out := true.B
        } .otherwise {
            op.push := io.op_in.push
            io.cmp_out := cmp_push_next
        }
        op.pop := false.B
    }
    
    temp := op.push
    io.op_out := op

    def ->(next: Block) = {
        next.io.op_in := this.io.op_out
        this.io.next_entry_in := next.io.entry_out
        next.io.cmp_in := this.io.cmp_out
    }
}