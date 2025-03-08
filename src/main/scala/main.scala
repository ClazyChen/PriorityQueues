import chisel3._

object Main extends App {
    println("Hello Chisel World")
    emitVerilog(new ShiftRegister(0, 16, 10))
    emitVerilog(new Systolic(0, 16, 10))
}