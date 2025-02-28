
import chisel3._

class SRBlock extends Module {
    val io = IO(new Bundle{
        val read = Input(Bool())
        val write = Input(Bool())
        val newEntryBus = Input(UInt(32.W))
        val leftEntry = Input(UInt(32.W))
        val rightEntry = Input(UInt(32.W))

        // TODO 这里图中是左右各有一个输出，这里直接一个输出
        // 然后接在左右两个block的输入里是否可行呢
        val outputEntry = Output(UInt(32.W))
    })

    val entry = RegInit(~0.U(32.W)) // 初始为全1，优先级最低
    io.outputEntry := entry
    /*
    * 三种情况
      1. read时接收左边的值并向右输出
      2. write时，若new entry比自己值小，则向左移动，否则向右移动
    * */
    when(io.read && !io.write) {
        entry := io.rightEntry
    }.elsewhen(!io.read && io.write) {
        // 自己的值比new entry大且右边的值比new entry大，则本entry向左移动
        // 自己的值比new entry大且右边的值比new entry小，则本entry向左移动且new entry应该存在本block
        // 自己的值比new entry小则位置不变
        // TODO 比较要修改
        when (entry > io.newEntryBus) {
            entry := Mux(io.rightEntry > io.newEntryBus, io.rightEntry, io.newEntryBus)
        }
    }.otherwise {

    }
}

class ShiftRegister extends Module {
    val io = IO(new Bundle{
        val read = Input(UInt(1.W))
        val write = Input(UInt(1.W))
        val newEntry = Input(UInt(32.W))
        val highestEntry = Output(UInt(32.W))
    })

    val newEntryBus = Wire(UInt(32.W))
    newEntryBus := io.newEntry
    val blocks = Seq.fill(10)(Module(new SRBlock()))

    blocks(9).io.leftEntry := ~0.U(32.W) // Invalid Entry
    blocks(9).io.rightEntry := blocks(8).io.outputEntry
    blocks(0).io.leftEntry := blocks(1).io.outputEntry
    blocks(0).io.rightEntry := 0.U(32.W)
    io.highestEntry := blocks(0).io.outputEntry

    for (i <- 1 until 9) {
        blocks(i).io.rightEntry := blocks(i - 1).io.outputEntry
        blocks(i).io.leftEntry := blocks(i + 1).io.outputEntry
    }

    for (i <- 0 until 10) {
        blocks(i).io.read := io.read
        blocks(i).io.write := io.write
        blocks(i).io.newEntryBus := newEntryBus
    }
}

object Main extends App {
    println("Hello Chisel World")
    emitVerilog(new ShiftRegister())
    val s = getVerilogString(new ShiftRegister())
    println(s)
}