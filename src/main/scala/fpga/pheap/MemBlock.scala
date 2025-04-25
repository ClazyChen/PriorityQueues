package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node
import fpga.mem

// 用trait实现对memory读写
trait MemoryTrait {
    // 从目标level的memory读数据
    def read (addr : UInt,cur_level : Int) : Node = {
        val r = getRPort
        r.en := true.B
        r.addr := addr
        r.data.asTypeOf(new Node(cur_level))
    }
    // 向memory写数据
    def write (addr : UInt,Node : node) : Unit = {
        val w = getWPort
        w.en := true.B
        r.addr := addr
        r.data := node.asUInt
    }
    // 停机
    def idle : Unit = {
        val r = getRPort
        val w = getWPort
        r.en := false.B
        w.en := false.B
        r.addr := DontCare
        w.addr := DontCare
        w.data := DontCare 
    }
}
// 每一层RPU内部的Memory
class MemBlock (val level : Int,val mem_type : String) extends Module with MemoryTrait { 
    // 计算addr_width和data_width
    val local_data_depth = UInt(1 << (level - 1))
    val local_data_width = (Node.asUInt).W
    val local_addr_width = log2Ceil(local_data_depth + 1) 
    // 对外暴露的接口
    val io = IO(new Bundle {
        val r = new ReadPort(local_addr_width, local_data_width) // 读
        val w = new WritePort(local_addr_width, local_data_width) // 写
    })
    // 根据需求选用不同的memory
    switch (mem_type) {
        is ("Sram") {
            val memory = new Sram(local_data_depth,local_data_width)
        }
        is ("SinglePortSram") {
            val memory = new SinglePortSram(local_data_depth,local_data_width)
        }
        is ("FFMem") {
            val memory = new FFMem(local_data_depth,local_data_width)
        }
        is ("SinglePortFFMem") {
            val memory = new SinglePortFFMem(local_data_depth,local_data_width)
        }
    }
    // 端口连接
    memory.io.r <> io.r
    memory.io.w <> io.w
    // 获取MemoryBlock端口
    def getRPort: ReadPort  = io.r
    def getWPort: WritePort = io.w
}
