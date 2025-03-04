
import chisel3._
//
//object Functions {
//    def cmpGreater(a: UInt, b: UInt): Bool = {
//        // TODO 比较器
//        return a > b
//    }
//}
//
//class Block(pWidth: Int, dataWidth : Int) extends Module {
//    val io = IO(new Bundle{
//        val read = Input(Bool())
//        val write = Input(Bool())
//        val nextRead = Output(Bool())
//        val nextWrite = Output(Bool())
//
//        val newEntry = Input(UInt(pWidth.W + dataWidth.W))
//        val lowEntry = Input(UInt(pWidth.W + dataWidth.W))
//
//        val outputEntry = Output(UInt(pWidth.W + dataWidth.W))
//        val outputNewEntry = Output(UInt(pWidth.W + dataWidth.W))
//    })
//
//    // 控制信号，用寄存器暂存一下，下个周期发送
//    val tmpRead = RegInit(false.B)
//    val tmpWrite = RegInit(false.B)
//    tmpRead := io.read
//    tmpWrite := io.write
//    io.nextRead := io.read
//    io.nextWrite := io.write
//
//    val entry = RegInit(~0.U(pWidth.W + dataWidth.W)) // 初始为全1，优先级最低
//    val tmpEntry = RegInit(~0.U(pWidth.W + dataWidth.W)) // 初始为全1，优先级最低
//
//    io.outputEntry := entry
//    io.outputNewEntry := tmpEntry
//
//
//
//    when(io.read && !io.write) {
//        // read时，用low的接替
//        entry := io.lowEntry
//    }.elsewhen(!io.read && io.write) {
//        // write时
//        // 若自己的值比new entry大且右边的值比new entry大，则本entry向左移动
//        // 若自己的值比new entry大且右边的值比new entry小，则本entry向左移动且new entry应该存在本block
//        // 若自己的值比new entry小则位置不变
//        when ( Functions.cmpGreater(entry(pWidth - 1, 0), io.newEntryBus(pWidth - 1, 0)) ) {
//            entry := Mux(Functions.cmpGreater(io.rightEntry(pWidth - 1, 0),
//                io.newEntryBus(pWidth - 1, 0)) , io.rightEntry, io.newEntryBus)
//        }
//    }.otherwise {
//        // 无操作
//    }
//}
//
///*
//* pWidth:  优先级位宽
//* dataWidth: 元数据位宽
//* blockCount: block个数
//* */
//class Systolic(pWidth: Int, dataWidth : Int, blockCount : Int) extends Module {
//    val io = IO(new Bundle{
//        val read = Input(UInt(1.W))
//        val write = Input(UInt(1.W))
//        val newEntry = Input(UInt(pWidth.W + dataWidth.W))
//        val highestEntry = Output(UInt(pWidth.W + dataWidth.W))
//    })
//
//    val newEntryBus = Wire(UInt(pWidth.W + dataWidth.W))
//    newEntryBus := io.newEntry
//
//    val blocks = Seq.fill(blockCount)(Module(new SRBlock(pWidth, dataWidth)))
//
//    // 最左边block的左输入为Invalid Entry
//    blocks(blockCount - 1).io.leftEntry := ~0.U(pWidth.W)
//
//    // 最右边block的右输入为0，表示最大优先级
//    blocks(0).io.rightEntry := 0.U(pWidth.W)
//
//    // 最右边block的输出则为整个PQ的输出
//    io.highestEntry := blocks(0).io.outputEntry
//
//    // 连接其他线
//    for (i <- 0 until blockCount) {
//        // 所有block都要连接这三个信号
//        blocks(i).io.read := io.read
//        blocks(i).io.write := io.write
//        blocks(i).io.newEntryBus := newEntryBus
//
//        // block相互连接，最左边和最右边block的特殊情况已处理
//        if (i != 0) {
//            blocks(i).io.rightEntry := blocks(i - 1).io.outputEntry
//        }
//        if (i != blockCount - 1) {
//            blocks(i).io.leftEntry := blocks(i + 1).io.outputEntry
//        }
//    }
//}
//
//object Main extends App {
//    println("Hello Chisel World")
//    emitVerilog(new ShiftRegister(16, 16, 10))
//}