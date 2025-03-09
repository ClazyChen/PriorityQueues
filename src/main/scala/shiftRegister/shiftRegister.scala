
import chisel3._

class ShiftRegisterBlock(dataWidth: Int, rWidth: Int) extends Module{
    val io = IO(new Bundle {
        val enqueue = Input(Bool())
        val dequeue = Input(Bool())
        val highEntry = Input(new Entry(dataWidth, rWidth))   // 上级的输入
        val lowEntry = Input(new Entry(dataWidth, rWidth))    // 下级的输入
        val highCmp = Input(Bool())                           // 上级的比较结果
        val newEntry = Input(new Entry(dataWidth, rWidth))    // 下级的输入
        val outputEntry = Output(new Entry(dataWidth, rWidth))
        val outputCmp = Output(Bool())
    })

    val entry = RegInit(Entry.default(dataWidth, rWidth))
    val cmp = WireInit(false.B) // 比较结果

    io.outputEntry := entry
    io.outputCmp := false.B

    when(io.dequeue && !io.enqueue) {
        entry := io.lowEntry
    }.elsewhen(!io.dequeue && io.enqueue) {
        cmp := entry.rank > io.newEntry.rank
        io.outputCmp := cmp

        when(cmp) {
            entry := Mux(io.highCmp, io.highEntry, io.newEntry)
        }
    }
}

class ShiftRegister(dataWidth: Int, rWidth: Int, capacity : Int) extends Module {
    val io = IO(new Bundle{
        val enqueue = Input(Bool())
        val dequeue = Input(Bool())
        val newEntry = Input(new Entry(dataWidth, rWidth))
        val highestEntry = Output(new Entry(dataWidth, rWidth))
    })

    val blockArray = Seq.fill(capacity)(Module(new ShiftRegisterBlock(dataWidth, rWidth)))

    for (i <- 0 until capacity) {
        blockArray(i).io.enqueue := io.enqueue
        blockArray(i).io.dequeue := io.dequeue
        blockArray(i).io.newEntry := io.newEntry

        if (i > 0) {
            blockArray(i).io.highEntry := blockArray(i - 1).io.outputEntry
            blockArray(i).io.highCmp := blockArray(i - 1).io.outputCmp
        }
        if (i < capacity - 1) blockArray(i).io.lowEntry := blockArray(i + 1).io.outputEntry
    }
    blockArray(0).io.highEntry := Entry.default(dataWidth, rWidth) // 这个输入不会产生作用
    blockArray(0).io.highCmp := false.B
    blockArray(capacity - 1).io.lowEntry := Entry.default(dataWidth, rWidth)

    io.highestEntry := blockArray(0).io.outputEntry
}