
import chisel3._

class ShiftRegisterBlock(val dataWidth: Int, val rankWidth: Int) extends Module{
    val io = IO(new Bundle {
        val enqueue = Input(Bool())
        val dequeue = Input(Bool())
        val highEntry = Input(new Entry(dataWidth, rankWidth))   // 上级的输入
        val lowEntry = Input(new Entry(dataWidth, rankWidth))    // 下级的输入
        val highCmp = Input(Bool())                              // 上级的比较结果
        val lowCmp = Input(Bool())                               // 下级的比较结果
        val newEntry = Input(new Entry(dataWidth, rankWidth))
        val outputEntry = Output(new Entry(dataWidth, rankWidth))
        val outputCmp = Output(Bool())
    })

    val entry = RegInit(Entry.default(dataWidth, rankWidth))
    val cmp = Wire(Bool()) // 比较结果
    cmp := entry.rank > io.newEntry.rank
    io.outputCmp := cmp
    io.outputEntry := entry

    when(io.dequeue && !io.enqueue) {
        entry := io.lowEntry
    }.elsewhen(!io.dequeue && io.enqueue) {
        when (cmp) {
            entry := Mux(io.highCmp, io.highEntry, io.newEntry)
        }
    }.elsewhen(io.dequeue && io.enqueue) {
        when (!cmp) {
            entry := Mux(io.lowCmp, io.newEntry, io.lowEntry)
        }
    }
}

class ShiftRegister(val dataWidth: Int, val rankWidth: Int, val capacity : Int) extends Module {
    val io = IO(new Bundle{
        val enqueue = Input(Bool())
        val dequeue = Input(Bool())
        val newEntry = Input(new Entry(dataWidth, rankWidth))
        val highestEntry = Output(new Entry(dataWidth, rankWidth))
    })

    val blockArray = Seq.fill(capacity)(Module(new ShiftRegisterBlock(dataWidth, rankWidth)))

    for (i <- 0 until capacity) {
        blockArray(i).io.enqueue := io.enqueue
        blockArray(i).io.dequeue := io.dequeue
        blockArray(i).io.newEntry := io.newEntry

        if (i > 0) {
            blockArray(i).io.highEntry := blockArray(i - 1).io.outputEntry
            blockArray(i).io.highCmp := blockArray(i - 1).io.outputCmp
        }
        if (i < capacity - 1) {
            blockArray(i).io.lowEntry := blockArray(i + 1).io.outputEntry
            blockArray(i).io.lowCmp := blockArray(i + 1).io.outputCmp
        }
    }
    blockArray(0).io.highEntry := DontCare
    blockArray(0).io.highCmp := false.B
    blockArray(capacity - 1).io.lowEntry := Entry.default(dataWidth, rankWidth)
    blockArray(capacity - 1).io.lowCmp := true.B

    io.highestEntry := blockArray(0).io.outputEntry
}