
import chisel3._

//def cmpGreater(a: UInt(c.W), b: UInt(d.W)): bool = {
//    //   比较最高位
//    //   0 - 0 = 1 <
//    //   1 - 0 >
//    //   0 - 1 <
//    //   1 - 1 = 1 >
//    //
//    val res = Wire()
//    res := a - b
//    return 1
//}

class SRBlock extends Module {
    val io = IO(new Bundle{
        val read = Input(Bool())
        val write = Input(Bool())
        val newEntryBus = Input(UInt(16.W))
        val leftEntry = Input(UInt(16.W))
        val rightEntry = Input(UInt(16.W))

        val outputEntry = Output(UInt(16.W))
    })

    val entry = RegInit(~0.U(16.W)) // 初始为全1，优先级最低
    io.outputEntry := entry

    when(io.read && !io.write) {
        // read时向右移动
        entry := io.leftEntry
    }.elsewhen(!io.read && io.write) {
        // write时
        // 若自己的值比new entry大且右边的值比new entry大，则本entry向左移动
        // 若自己的值比new entry大且右边的值比new entry小，则本entry向左移动且new entry应该存在本block
        // 若自己的值比new entry小则位置不变
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
        val newEntry = Input(UInt(16.W))
        val highestEntry = Output(UInt(16.W))
    })

    val newEntryBus = Wire(UInt(16.W))
    newEntryBus := io.newEntry

    val size = 10 // SR的规模

    val blocks = Seq.fill(size)(Module(new SRBlock()))

    // 最左边block的左输入为Invalid Entry
    blocks(size - 1).io.leftEntry := ~0.U(16.W)

    // 最右边block的右输入为0，表示最大优先级
    blocks(0).io.rightEntry := 0.U(16.W)

    // 最右边block的输出则为整个PQ的输出
    io.highestEntry := blocks(0).io.outputEntry

    // 连接其他线
    for (i <- 0 until size) {
        // 所有block都要连接这三个信号
        blocks(i).io.read := io.read
        blocks(i).io.write := io.write
        blocks(i).io.newEntryBus := newEntryBus

        // block相互连接，最左边和最右边block的特殊情况已处理
        if (i != 0) {
            blocks(i).io.rightEntry := blocks(i - 1).io.outputEntry
        }
        if (i != size - 1) {
            blocks(i).io.leftEntry := blocks(i + 1).io.outputEntry
        }
    }
}

object Main extends App {
    println("Hello Chisel World")
    emitVerilog(new ShiftRegister())
    val s = getVerilogString(new ShiftRegister())
    println(s)
}