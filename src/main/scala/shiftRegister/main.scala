
import chisel3._

class ShiftRegister(dataWidth: Int, pWidth: Int, blockCount : Int) extends Module {
    val io = IO(new Bundle{
        val read = Input(Bool())
        val write = Input(Bool())
        val newEntry = Input(new Entry(dataWidth, pWidth))
        val highestEntry = Output(new Entry(dataWidth, pWidth))
    })

    val entryArray = RegInit(VecInit(Seq.fill(blockCount)(Entry(dataWidth, pWidth) )))
    val cmpArray = Wire(Vec(blockCount, Bool())) // 暂存比较结果

    for (i <- 0 until blockCount) {
        cmpArray(i) := false.B
    }

    when(io.read && !io.write) {
        // read时向右移动
        for (i <- 0 until blockCount - 1) {
            entryArray(i) := entryArray(i + 1)
        }
        entryArray(blockCount - 1) := Entry(dataWidth, pWidth)
    }.elsewhen(!io.read && io.write) {
        // 先暂存比较结果
        for (i <- 0 until blockCount) {
            cmpArray(i) := Functions.cmpGreater(entryArray(i).priority, io.newEntry.priority)
        }

        // 如果优先级允许等于最大值，那么这里会导致无法插入优先级等于最大值的元素，因为优先级相等时按先后顺序
        // 这里当它不允许了

        // 对0号元素单独处理
        when(cmpArray(0)) {
            entryArray(0) := io.newEntry
        }

        // 对其余元素处理
        for (i <- 1 until blockCount) {
            when (cmpArray(i)) {
                entryArray(i) := Mux(cmpArray(i - 1), entryArray(i - 1), io.newEntry)
            }
        }
    }.otherwise {
        // 无操作
    }

    io.highestEntry := entryArray(0)
}

object Main extends App {
    println("Hello Chisel World")
    emitVerilog(new ShiftRegister(0, 16, 10))
    //emitVerilog(new ALU)
}