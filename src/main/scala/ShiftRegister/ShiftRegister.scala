package ShiftRegister

import chisel3._
import chisel3.util._

/** ShiftRegister
 * @param n     寄存器的级数
 * @param width 每个寄存器的位宽
 */
class ShiftRegister(n: Int, width: Int) extends Module {
  //定义了此模块的输入与输出接口
  val io = IO(new Bundle {
    val in = Input(UInt(width.W))
    val load = Input(UInt((n * width).W))
    val out = Output(UInt(width.W))
    val control = Input(UInt(2.W))                //接收一个2位的控制信号：0保持、1左移、2右移、3并行加载
    val dataOut = Output(Vec(n, UInt(width.W)))   //输出整个寄存器数组
  })

  // 定义并初始化寄存器数组，初始值为0
  val regs = RegInit(VecInit(Seq.fill(n)(0.U(width.W))))
  val nextRegs = Wire(Vec(n, UInt(width.W)))       //计算每个寄存器下一时刻的值，默认保持当前值
  for(i <- 0 until n) {
    nextRegs(i) := regs(i)
  }

  switch(io.control){
    is(0.U){ }   //控制信号0: 保持现状
    is(1.U){    //控制信号1: 左移
      nextRegs(0) := io.in   //从输入中得到数据
      for(i <- 1 until n){
        nextRegs(i) := regs(i-1)
      }
    }
    is(2.U){    //控制信号2: 右移
      nextRegs(n-1) := io.in
      for(i <- 0 until n-1){
        nextRegs(i) := regs(i+1)
      }
    }
    is(3.U) {   //控制信号3: 并行加载：将一个拼接后的大数据加载到各个寄存器中
      for(i <- 0 until n) {
        nextRegs(i) := io.load((i + 1) * width - 1, i * width) // 将输入 load 按 width 位进行切片
      }
    }
  }

  regs := nextRegs
  io.out := regs(n - 1)
  io.dataOut := regs

}
