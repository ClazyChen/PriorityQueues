
import chisel3._

class ShiftRegister(dataWidth: Int, rWidth: Int, capacity : Int) extends Module {
    val io = IO(new Bundle{
        val dequeue = Input(Bool())
        val enqueue = Input(Bool())
        val newEntry = Input(new Entry(dataWidth, rWidth))
        val highestEntry = Output(new Entry(dataWidth, rWidth))
    })

    val entryArray = RegInit(VecInit(Seq.fill(capacity)(Entry.default(dataWidth, rWidth) )))
    val cmpArray = WireInit(VecInit(Seq.fill(capacity)(false.B))) // 暂存比较结果

    when(io.dequeue && !io.enqueue) {
        // dequeue时向右移动
        for (i <- 0 until capacity - 1) {
            entryArray(i) := entryArray(i + 1)
        }
        entryArray(capacity - 1) := Entry.default(dataWidth, rWidth)
    }.elsewhen(!io.dequeue && io.enqueue) {
        // 先暂存比较结果
        for (i <- 0 until capacity) {
            cmpArray(i) := entryArray(i).rank > io.newEntry.rank
        }

        // 对0号元素单独处理
        when(cmpArray(0)) {
            entryArray(0) := io.newEntry
        }

        // 对其余元素处理
        for (i <- 1 until capacity) {
            when (cmpArray(i)) {
                entryArray(i) := Mux(cmpArray(i - 1), entryArray(i - 1), io.newEntry)
            }
        }
    }.otherwise {
        // 无操作
    }

    io.highestEntry := entryArray(0)
}