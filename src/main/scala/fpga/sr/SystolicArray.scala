package fpga.sr

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._

class SystolicArray extends Module with PriorityQueueTrait {
    // val io = IO(new Bundle {
    //     val read  = Input(Bool())   
    //     val write = Input(Bool())  
    //     val new_entry = Input(new Entry(dataWidth, rankWidth))
    //     val output = Output(new Entry(dataWidth, rankWidth))
    // })
    val io = IO(new PQIO)

    // val blocks = Seq.fill(blocksNum)(Module(new SystolicBlock(dataWidth, rankWidth)))
    val blocks = Seq.fill(count_of_entries)({
        val block = Module(new SystolicBlock)
        block
    })

    // io.output := blocks(0).io.output
    io.entry_out := blocks.head.io.entry_out

    // blocks(0).io.enq_in := io.write
    // blocks(0).io.deq_in := io.read
    // blocks(0).io.bubble_in := io.new_entry
    blocks.head.io.op_in := io.op_in
    
    // if (blocksNum > 1) {
    //     blocks(0).io.left := blocks(1).io.output
    // } 
    // else {
    //     blocks(0).io.left := Entry.default(dataWidth, rankWidth)
    // } 

    // for(i <- 1 until blocksNum) {
    //     // blocks(i).io.enq_in := blocks(i - 1).io.enq_out
    //     // blocks(i).io.deq_in := blocks(i - 1).io.deq_out
    //     blocks(i).io.op_in := blocks(i - 1).io.op_out

    //     blocks(i).io.next_entry_in := blocks(i - 1).io.entry_out
    //     if (i < blocksNum - 1) {
    //     blocks(i).io.left := blocks(i + 1).io.output
    //     }
    //     else {
    //     blocks(i).io.left := Entry.default
    //     }
    // }
    for(i <- 0 until count_of_entries - 1) {
        blocks(i) -> blocks(i + 1)
    }
    blocks.last.io.next_entry_in := DontCare

}