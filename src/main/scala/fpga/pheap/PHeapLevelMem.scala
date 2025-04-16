package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.pheap.Const._

// implement the io interface of each level in PHeap
class PHeapMemLevelIO(val level: Int, val data_width: Int) extends Bundle {
  val r = new ReadPort(level, data_width)
  val w = new WritePort(level, data_width)
}

// implement the each level memory in PHeap
class PHeapLevelMem(val level: Int, val mem_impl: String) extends Module with DualPortMemoryImpl{
  val addr_width = if(level <= 1) 1 else log2Ceil(level)

  val io = IO(new PHeapMemLevelIO(level, data_width))

  // 底层存储器：同步读写
  val mem = SyncReadMem(level, UInt(data_width.W))

  // 读端口先置为 DontCare，稍后根据条件赋值
  io.r.data := DontCare
  // 读操作：SyncReadMem 有一拍延迟
  when (io.r.en) {
    // 发起读请求
    io.r.data := mem.read(io.r.addr)
  }
  // 写操作
  when (io.w.en) {
    mem.write(io.w.addr, io.w.data)
  }
  // 处理读写冲突（同地址同周期的写，下一周期读应该读到新数据而非旧数据）
  val sameAddrDelay = RegNext(io.r.en && io.w.en && io.r.addr === io.w.addr)
  val wdataDelay    = RegNext(io.w.data)
  when (sameAddrDelay) {
    io.r.data := wdataDelay
  }
  // provide the IO interface
  def getRPort: ReadPort = io.r
  def getWPort: WritePort = io.w
}