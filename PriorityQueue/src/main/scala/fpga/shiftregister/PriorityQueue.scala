package fpga.shiftregister

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

// top-module : PriorityQueue Block
class PriorityQueue extends Module with PriorityQueueTrait {
    
    // 实现trait中定义的IO接口 
    val io = IO(new PQIO)

    val blocks = Seq.fill(count_of_entries)(Module(new Block))

    // 所有的block都连接到新到达的entry线
    for (i <- 0 until count_of_entries) {
        blocks(i).io.op_in := io.op_in
    }

    // 模块串联
    for (i <- 0 until (count_of_entries - 1)) {
        blocks(i + 1).io.prev_entry_in <> blocks(i).io.entry_out
        blocks(i).io.next_entry_in <> blocks(i + 1).io.entry_out
        blocks(i + 1).io.prev_cmp_in <> blocks(i).io.cmp_out
        blocks(i).io.next_cmp_in <> blocks(i + 1).io.cmp_out
    }

    // 无关端口赋值
    blocks(count_of_entries - 1).io.next_entry_in := Entry.default
    blocks(count_of_entries - 1).io.next_cmp_in := true.B

}