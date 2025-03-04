
import chisel3._

object Functions {
    // >
    def cmpGreater(a: UInt, b: UInt): Bool = {
        return (b -& a)(a.getWidth)
    }
}

/*
* pWidth:  优先级位宽
* dataWidth: 元数据位宽
* blockCount: block个数
* */
class ShiftRegister(pWidth: Int, dataWidth : Int, blockCount : Int) extends Module {
    val io = IO(new Bundle{
        val read = Input(Bool())
        val write = Input(Bool())
        val newEntry = Input(UInt(pWidth.W + dataWidth.W))
        val highestEntry = Output(UInt(pWidth.W + dataWidth.W))
    })

    // 初始为全1，优先级最低
    val entryArray = RegInit(VecInit(Seq.fill(blockCount)(~0.U(pWidth.W + dataWidth.W))))
    val cmpArray = Wire(Vec(blockCount, Bool()))

    // TODO：这里不初始化会报错，为什么
    for (i <- 0 until blockCount) {
        cmpArray(i) := false.B
    }

    when(io.read && !io.write) {
        // read时向右移动
        for (i <- 0 until blockCount - 1) {
            entryArray(i) := entryArray(i + 1)
        }
        entryArray(blockCount - 1) := ~0.U(pWidth.W + dataWidth.W)
    }.elsewhen(!io.read && io.write) {
        // 先暂存比较结果
        for (i <- 0 until blockCount) {
            cmpArray(i) := Functions.cmpGreater(entryArray(i)(pWidth - 1, 0), io.newEntry(pWidth - 1, 0))
        }

        // 如果优先级允许等于~0，那么这里会导致无法插入优先级等于~0的元素，因为优先级相等时按先后顺序
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
    emitVerilog(new ShiftRegister(16, 16, 10))
    //emitVerilog(new ALU)
}