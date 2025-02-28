package ShiftRegister

import chisel3._
import chisel3.util._

class ShiftRegister(width: Int) extends Module {
  //定义了此模块的输入与输出接口
  val io = IO(new Bundle {
    val in = Input(Bool())
    val out = Output(UInt(width.W))
  })

  //定义了寄存器reg，类型为无符号整数，位宽为width
  val reg = RegInit(0.U(width.W))

  //实现了右移寄存器，每次将原数据的低width-1位和输入进行拼接
  reg := Cat(reg(width-2, 0), io.in)
  io.out := reg
}
