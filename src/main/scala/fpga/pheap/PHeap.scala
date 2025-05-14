package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._


class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val level = if (pheap_level > 0) pheap_level else log2Ceil(count_of_entries + 1)

    val RPUs = Seq.tabulate(level)(i => Module(new RPU(i + 1, pheap_mem)))

    // token输入
    val token            = Wire(new Token(1))
    token.op            := io.op_in
    token.position      := 1.U
    RPUs(0).io.token_in := token

    // RPU0不需要被上层读
    RPUs(0).io.read_in      := false.B
    RPUs(0).io.read_addr_in := DontCare

    // pop/replace输出，在信号输入时，data out就是根节点的最新数据
    io.entry_out := RPUs(0).io.data_out.asTypeOf(Vec(2, new Node(1)))(1).entry

    // connect RPUs one by one
    for (i <- 0 until level - 1) {
        RPUs(i) ~> RPUs(i + 1)
    }
    
    RPUs.last.io.next_data_in := VecInit(Seq.fill(2)(Node.default(level + 1))).asUInt
}