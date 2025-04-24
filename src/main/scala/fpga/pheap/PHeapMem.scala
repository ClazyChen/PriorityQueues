package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.pheap.Const._

class PHeapMemIO extends Bundle {
  val read_signal = Input(Bool())
  val write_signal = Input(Bool())
  val operation_level = Input(UInt(log2Ceil(count_of_levels).W))
  val operation_local_addr = Input(UInt(count_of_levels.W))
  val data_in = Input(UInt(data_width.W))
  val data_out = Output(UInt(data_width.W))
}

// 顶层 PHeapMem 模块的实现
class PHeapMem(val mem_impl: String) extends Module {
  // 生成每个层级所需地址宽度，假设 count_of_levels 与 data_width 均在外部定义
  val addr_widths: Seq[Int] = (1 to count_of_levels).map { level =>
    log2Ceil(level)
  }

  // 定义顶层 IO
  val io = IO(new PHeapMemIO)

  // 生成各个层次的存储器模块，层级编号从 1 到 count_of_levels
  val levelMems: Seq[PHeapLevelMem] = (1 to count_of_levels).map { i =>
    Module(new PHeapLevelMem(i, mem_impl))
  }

  // 根据 addr_widths 数组逐层生成各层的读端口集合
  val read_ports: Seq[ReadPort] = addr_widths.map { width =>
    Wire(new ReadPort(width, data_width))
  }
  // 根据 addr_widths 数组逐层生成各层的写端口集合
  val write_ports: Seq[WritePort] = addr_widths.map { width =>
    Wire(new WritePort(width, data_width))
  }

  // 将顶层读写请求路由到各层
  for (i <- 0 until count_of_levels) {
    // 对于读端口：当选择的操作层级等于 (i+1) 时使能读操作
    val read_enable = (io.operation_level === (i + 1).U) && io.read_signal
    read_ports(i).en   := read_enable
    // 根据当前层地址宽度截取地址信号（低位部分）
    read_ports(i).addr := io.operation_local_addr(read_ports(i).addr.getWidth - 1, 0)

    // 对于写端口：当选择的操作层级等于 (i+1) 时使能写操作
    val write_enable = (io.operation_level === (i + 1).U) && io.write_signal
    write_ports(i).en   := write_enable
    write_ports(i).addr := io.operation_local_addr(write_ports(i).addr.getWidth - 1, 0)
    write_ports(i).data := io.data_in

    // 将生成的端口与相应的子模块连接
    levelMems(i).io.r <> read_ports(i)
    levelMems(i).io.w <> write_ports(i)
  }

  // 根据操作层级，从各层的读端口中选择相应的数据输出
  io.data_out := Mux1H(
    (0 until count_of_levels).map(i =>
      ((io.operation_level === (i+1).U) -> read_ports(i).data)
    )
  )

}
