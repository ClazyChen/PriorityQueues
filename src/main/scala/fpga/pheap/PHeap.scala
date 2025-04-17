package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._


class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val RPUs = Seq.tabulate(pheap_level)(i => Module(new RPU(i + 1, pheap_mem)))

    // 启动信号，根据操作是否有效
    val enable            = io.op_in.push.existing | io.op_in.pop
    RPUs(0).io.enable_in := enable

    // token输入
    val token            = Wire(new Token(1))
    token.op            := io.op_in
    token.position      := 1.U
    RPUs(0).io.token_in := token

    // RPU0不需要被上层读
    RPUs(0).io.read_in      := false.B
    RPUs(0).io.read_addr_in := DontCare

    // pop/replace输出，输入之后的第二个周期产生输出
    // 而sr,sa 输出放在第一个block，所以提前产生了
    // 这里也可以考虑第一层用寄存器存放，这样就可以早点输出了
    val entry     = RPUs(0).io.entry_out.getOrElse(DontCare)
    io.entry_out := entry

    // connect RPUs one by one
    for (i <- 0 until pheap_level - 1) {
        RPUs(i) ~> RPUs(i + 1)
    }
    
    RPUs.last.io.read_dual_node_in := VecInit(Seq.fill(2)(Node.default(pheap_level + 1))).asUInt
}