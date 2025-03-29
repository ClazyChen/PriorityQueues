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

    // 测试过程中存在以下问题：
    // 如果每个block都含有元素，就可能导致溢出问题，但是因为含有temp，所以实际能存储的元素大于count_of_entries
    // SystolicArray满时，软件测试的结果不一定可靠
    val entry = RegInit(Entry.default)
    val temp = RegInit(Entry.default)

    val op = RegInit(Operator.default)

    io.entry_out := entry
    io.op_out.push := temp
    io.temp_out := temp

    val cmp = io.op_in.push < entry

    def min(a: Entry, b: Entry, c: Entry): Entry = {
        // 第一阶段比较前两个Entry
        val minAB = Mux(a < b, a, b)
        
        // 第二阶段比较中间结果与第三个Entry
        Mux(minAB < c, minAB, c)
    }



    when(io.op_in.pop) {
        // 假设没有replace
        when(io.op_in.push.existing) {
            when(io.op_in.push < entry) {
                temp := Entry.default
                op.push := Entry.default
                op.pop := false.B
            } .otherwise {
                entry := min(io.op_in.push, temp, io.next_entry_in)
                temp := io.op_in.push
                op.push := io.op_in.push
                op.pop := true.B
            }
        } .otherwise {
            entry := Mux(temp < io.next_entry_in, temp, io.next_entry_in)
            temp := Entry.default
            op.push := Entry.default
            op.pop := true.B
        }
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