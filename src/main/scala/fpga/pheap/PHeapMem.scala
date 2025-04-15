package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.pheap.Const._

// The trait of a memory
trait BHeapMemoryTrait {
  // read and return the entry at addr
  def read_block(addr: UInt, level: Int): BNode
  // write data to the entry at addr
  def write_block(addr: UInt, bnode: BNode): Unit
  // idle the memory
  def idle(): Unit
}

trait BHeapMemImpl extends BHeapMemoryTrait {
  // get the IO interface from the module
  def getRPort: ReadPort
  def getWPort: WritePort
  def read_block(addr: UInt, level: Int): BNode = {
    val rport = getRPort
    rport.en := true.B
    rport.addr := addr
    rport.data.asTypeOf(new BNode(level))
  }
  def write_block(addr: UInt, bnode: BNode): Unit = {
    val wport = getWPort
    wport.en := true.B
    wport.addr := addr
    wport.data := bnode.asUInt
  }
  def idle(): Unit = {
    val rport = getRPort
    val wport = getWPort
    rport.en := false.B
    wport.en := false.B
    rport.addr := DontCare
    wport.addr := DontCare
    rport.data := DontCare
  }
}

// 使用 SyncReadMem 实现伪双端口内存，并混入 BHeapMemImpl
class PHeapMem(val level: Int, val mem_impl: String) extends Module with BHeapMemImpl {
  val addr_width = log2Ceil(level)

  // 对外暴露读写端口
  val io = IO(new Bundle {
    val r = new ReadPort(addr_width, data_width)
    val w = new WritePort(addr_width, data_width)
  })

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

   // 实现 BHeapMemImpl 所需的 getRPort / getWPort
  def getRPort: ReadPort  = io.r
  def getWPort: WritePort = io.w
}