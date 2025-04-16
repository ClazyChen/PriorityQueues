package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.node
import fpga.pheap.RPU

// top-module : P-Heap
class PHeap (mem_types : Seq[String]) extends Module {
    // IO实例化
    val io = IO(new PQIO)

    // 根据参数生成RPUs
    val rpus = Seq.tabulate(generate_level(count_of_entries)) { i =>
        mem_types(i) match {
            case "Sram" => Module(new RPU(i + 1,"Sram"))
            case "SinglePortSram"  => Module(new RPU(i + 1,"SinglePortSram"))
            case "FFMem" => Module(new RPU(i + 1,"FFMem"))
            case "SinglePortFFMem" => Module(new RPU(i + 1,"SinglePortFFMem"))
            case unknown => throw new IllegalArgumentException(s"未知存储器类型在第 ${i + 1} 层: $unknown")
        }
    }

    // RPUs 模块连接
    // rpu[0]代表第一块sram，level为1
    for (i <- 0 until (generate_level(count_of_entries) - 1)) {
        rpus(i) ~> rpus(i + 1)
    }
    
    // 连接到外部
    rpus.head.token_in.op := io.op_in
    io.entry_out := rpus.head.mem_out.value

}

