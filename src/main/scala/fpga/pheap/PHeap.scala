package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.mem._
import fpga.pheap.Param._


class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    val rpus = Seq.tabulate(count_of_levels + 1) { level =>
        val level_module = Module(new RPU(level))
        level_module
    }

    io.entry_out := rpus(1).io.left_node_out.entry
    
    // rpus.head.io.token_in := TokenNode.init(0)
    // rpus.head.io.parent_pos_in := DontCare
    // rpus.head.io.lc_node_in := DontCare
    // rpus.head.io.rc_node_in := DontCare

    rpus(1).io.parent_pos_in := 0.U

    when(io.op_in.pop && io.op_in.push.existing && rpus(1).io.left_node_out.entry < io.op_in.push) {
        rpus(1).io.token_in := TokenNode.init(1)
    } .otherwise {
        rpus(1).io.token_in := TokenNode.init(1,io.op_in.push, io.op_in)
    }

    rpus.last.io.lc_node_in := DontCare
    rpus.last.io.rc_node_in := DontCare

    // for (i <- 1 until count_of_levels) {
    //     rpus(i) ~> rpus(i + 1)
    // }

    // PHeap.scala 中
    // 1) head 特殊接法（无前驱）
    rpus(0).io.token_in      := TokenNode.init(0)
    rpus(0).io.parent_pos_in := 0.U
    rpus(0).io.lc_node_in    := DontCare
    rpus(0).io.rc_node_in    := DontCare

    // 2) 显式打拍链接
    for (i <- 0 until count_of_levels) {
    // 建四个寄存器，分别对应一对一方向
        val token_reg      = RegNext(rpus(i).io.token_out,     TokenNode.init(i+1))
        val parent_reg     = RegNext(rpus(i).io.cur_pos_out,    0.U)
        val left_node_reg  = RegNext(rpus(i+1).io.left_node_out,  Node.default(i))
        val right_node_reg = RegNext(rpus(i+1).io.right_node_out, Node.default(i))

        // 然后再一次性把它们连到 IO 上
        rpus(i+1).io.token_in     := token_reg
        rpus(i+1).io.parent_pos_in:= parent_reg

        rpus(i).io.lc_node_in     := left_node_reg
        rpus(i).io.rc_node_in     := right_node_reg
    }


    // if(debug) {
    //      io.dbgPort.foreach { dbgPort =>
    //          dbgPort := blocks.map(_.io.entry_out)
    //      }
    //  }
}
