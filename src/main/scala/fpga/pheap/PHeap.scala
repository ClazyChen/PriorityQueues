package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.node
import fpga.pheap.RPU
import fpga.pheap.Func._


// top-module : P-Heap
// 设置level为参数，指定时，使用这个level，不指定时，动态计算最少level
class PHeap (mem_types : Seq[String], total_level : Int = 0) extends Module {
    
    val io = IO(new PQIO)

    // 获取level,根据具体的输入
    when (!total_level)  {
        val total_levels = generate_level(count_of_entries)
    }.otherwise {
        val total_levels = count_of_levels
    }

    // 根据参数生成RPUs
    // rpus(0) 对应 RPU level1
    val rpus = Seq.tabulate(total_levels) { i =>
        mem_types(i) match {
            case "Sram" => Module(new RPU(i + 1,"Sram"))
            case "SinglePortSram"  => Module(new RPU(i + 1,"SinglePortSram"))
            case "FFMem" => Module(new RPU(i + 1,"FFMem"))
            case "SinglePortFFMem" => Module(new RPU(i + 1,"SinglePortFFMem"))
            case unknown => throw new IllegalArgumentException(s"未知存储器类型在第 ${i + 1} 层: $unknown")
        }
    }

    // RPUs 模块连接
    for (i <- 0 until (total_levels - 1)) {
        rpus(i) ~> rpus(i + 1)
    }
    
    // 连接到外部
    rpus.head.token_in.op := io.op_in

}
