package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node
import fpga.mem
import fpga.pheap.Func._


// 尝试做一个带一个周期读延迟的FFMem，时序特性向Sram靠拢
class PheapFFMem extends Module {
    
}