//
import chisel3._

/*
* pWidth:  优先级位宽
* dataWidth: 元数据位宽
* capacity: 容量
* */
class Systolic(dataWidth : Int, pWidth: Int, capacity : Int) extends Module {
    val io = IO(new Bundle{
        val dequeue = Input(Bool())
        val enqueue = Input(Bool())
        val newEntry = Input(new Entry(dataWidth, pWidth))
        val highestEntry = Output(new Entry(dataWidth, pWidth))
    })

    val capacity_ = capacity + 1  // 0号元素不算入容量
    val state = RegInit(0.U(1.W)) // 控制脉动状态

    // B是temp register
    val entryArrayA = RegInit(VecInit(Seq.fill(capacity_)(Entry(dataWidth, pWidth))))
    val entryArrayB = RegInit(VecInit(Seq.fill(capacity_)(Entry(dataWidth, pWidth))))

    io.highestEntry := entryArrayA(0)

    // state = 1 奇数项脉动，state = 0 偶数项脉动，交替执行
    state := ~state

    for (i <- 1 until capacity_) {
        // 即A(i - 1) B(i - 1)  A(i)比较，其中已知A(i - 1) <= B(i - 1)
        // 按序赋值到A(i - 1) A(i) B(i) 处
        when (state === (i % 2).U) {
            when(Functions.cmpGreater(entryArrayA(i).priority, entryArrayB(i - 1).priority)) {
                // A(i-1) <= B(i-1) <= A(i)
                entryArrayA(i) := entryArrayB(i - 1)
                entryArrayB(i) := entryArrayA(i)
            }.elsewhen(Functions.cmpGreater(entryArrayA(i - 1).priority, entryArrayA(i).priority)) {
                // A(i) <= A(i-1) <= B(i-1)
                entryArrayA(i - 1) := entryArrayA(i)
                entryArrayA(i) := entryArrayA(i - 1)
                entryArrayB(i) := entryArrayB(i - 1)
            }.otherwise {
                // A(i-1) <= A(i) <= B(i-1)
                entryArrayB(i) := entryArrayB(i - 1)
            }
        }
    }

    // 偶数项脉动时才能处理输入，这样block2、4、6、8...发生脉动时刚好block0也写入
    when (state === 0.U) {
        when(io.enqueue && !io.dequeue) {
            entryArrayA(0).priority := 0.U
            entryArrayB(0) := io.newEntry
        }.elsewhen(!io.enqueue && io.dequeue) {
            entryArrayA(0).priority := -1.S(pWidth.W).asUInt
            entryArrayB(0).priority := -1.S(pWidth.W).asUInt
        }
    }
}