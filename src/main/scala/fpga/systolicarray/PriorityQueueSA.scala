package fpga.systolicarray

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class PriorityQueueSA extends Module with PriorityQueueTrait {

    // 实例化IO接口
    val io = IO(new PQIO)

    // 实例化block
    val blocks = Seq.fill(count_of_entries)(Module(new Block))

    // 模块串联
    for (i <- 0 until (count_of_entries - 1)) {
        blocks(i + 1).io.op_in <> blocks(i).io.op_out
        blocks(i).io.next_entry_in <> blocks(i + 1).io.entry_out
    }

    // 无关端口赋值
    blocks(count_of_entries - 1).io.next_entry_in := Entry.default

    // 连接到上级模块
    io.entry_out := blocks(0).io.entry_out
    blocks(0).io.op_in := io.op_in

    

}
