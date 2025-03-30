package fpga.systolicarray

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// identical blocks 
// two registers (entry and temp) 
// mutiplexer
// comparator
// decision logic

// 定义脉动阵列Block结构
class Block extends Module {
    val io = IO(new Bundle {
       val op_in = Input(new Operator) // 当前块接收前一个块的输入/系统外部输入     
       val op_out = Output(new Operator) // 当前块向下一个块传递的operator      
       val next_entry_in = Input(new Entry) // 当前块接收到下一个块的输入
       val entry_out = Output(new Entry) // 对外输出的entry
    })

    // 初始化两个寄存器
    val entry_holder = RegInit(Entry.default)
    val tmp_holder = RegInit(Entry.default)

    // 初始化输出端口
    io.op_out.push := Entry.default
    io.op_out.pop := false.B
    io.entry_out := entry_holder

    // 用变量保存rank比较状态
    val cmp_status = io.op_in.push < entry_holder

    // 定义一个状态计算函数
    def compute_forward_state(forward_entry : Entry) : Entry = {
         val next_entry_state = Wire(new Entry)
         next_entry_state.existing := forward_entry.existing
         next_entry_state.metadata := forward_entry.metadata
         next_entry_state.rank := forward_entry.rank
         next_entry_state
    }

    // decision logic
    when (io.op_in.push.existing) { // 对应push操作，先不实现replace操作
        when(cmp_status) { // cmp_status 为 true时
            val next_entry_state = compute_forward_state(entry_holder) // 超前计算传递出去的状态
            tmp_holder := entry_holder
            entry_holder := io.op_in.push
            io.op_out.push := next_entry_state
            io.op_out.pop := false.B
        }
        .otherwise { // cmp_status 为 false时
            val next_entry_state = compute_forward_state(io.op_in.push) // 超前计算传递出去的状态
            tmp_holder := io.op_in.push
            io.op_out.push := next_entry_state
            io.op_out.pop := false.B
        }
    }
    .elsewhen (io.op_in.pop) { // 对应pop操作 
        io.op_out.push := Entry.default
        io.op_out.pop := true.B
        entry_holder := io.next_entry_in
    }
    .otherwise {
        // do nothing
    }
    

}                                                                            