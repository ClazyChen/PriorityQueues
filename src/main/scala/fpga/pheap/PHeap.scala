package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Entry._
import fpga.pheap.RPU
import fpga.pheap.Func._

// top-module : P-Heap
class PHeap extends Module with PriorityQueueTrait {

    val io = IO(new PQIO)

    // calculate total levels
    val total_levels =  if (count_of_levels != 0) {
        count_of_levels
    } else {
        generate_level(count_of_entries)
    }

    // generate rpus
    val rpus = Seq.tabulate(total_levels) { i => Module(new RPU(i + 1)) }

    // 最上层的token，传入第一层RPU
    val first_token = Wire(new Token(1))
    first_token.op := io.op_in
    first_token.position := 1.U
    rpus.head.io.token_in := first_token

    // pheap初始化
    rpus.head.io.read_in := false.B
    rpus.head.io.read_addr_in := DontCare
    rpus.last.io.next_data_in := VecInit(Seq.fill(2)(Node.default(total_levels + 1))).asUInt

    // 输出端口
    io.entry_out := rpus(0).io.data_out.asTypeOf(Vec(2, new Node(1)))(1).value

    // rpus模块连接
    for (i <- 0 until (total_levels - 1)) {
        rpus(i) ~> rpus(i + 1)
    }
}