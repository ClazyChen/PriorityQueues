import chisel3._

class Entry(dataWidth: Int, rankWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val rank = UInt(rankWidth.W)
}

object Entry {
  def default(dataWidth: Int, rankWidth: Int): Entry = {
    val e = Wire(new Entry(dataWidth, rankWidth))
    // 默认rank为最低优先级
    e.rank := -1.S(rankWidth.W).asUInt
    e.data := 0.U(dataWidth.W)
    e
  }
}

class Block(dataWidth: Int, rankWidth: Int) extends Module {
  val io = IO(new Bundle {
    val read = Input(Bool())
    val write = Input(Bool())
    val new_entry = Input(new Entry(dataWidth, rankWidth))
    val left = Input(new Entry(dataWidth, rankWidth))
    val right = Input(new Entry(dataWidth, rankWidth))
    val output = Output(new Entry(dataWidth, rankWidth))
  })

  val entry = RegInit(Entry.default(dataWidth, rankWidth))
  io.output := entry

  when(io.read && !io.write) {
    // dequeue操作,entry依次右移
    entry := io.left
  }.elsewhen(!io.read && io.write) { // enqueue操作
    when(entry.rank > io.new_entry.rank) { // 当前优先级小于新entry优先级
      when(io.new_entry.rank > io.right.rank) { // entry > new_entry > right
        entry := io.new_entry // 当前位置就是新entry最终位置
      }.otherwise {
        entry := io.right // 新entry在右侧插入,新entry左侧元素左移
      }
    } // 当前优先级高于新entry优先级,不移动
  }
}

class ShiftRegister(dataWidth: Int, rankWidth: Int, blocksNum: Int)
    extends Module {
  val io = IO(new Bundle {
    val read = Input(Bool())
    val write = Input(Bool())
    val new_entry = Input(new Entry(dataWidth, rankWidth))
    val output = Output(new Entry(dataWidth, rankWidth))
  })

  val blocks = Seq.fill(blocksNum)(Module(new Block(dataWidth, rankWidth)))
  io.output := blocks(0).io.output
  blocks(0).io.right := Entry.default(dataWidth, rankWidth)
  // 0th block右侧entry优先级设为最高
  blocks(0).io.right.rank := 0.U

  // 最左block的左侧entry优先级初始化为最低
  blocks(blocksNum - 1).io.left := Entry.default(dataWidth, rankWidth)

  for (i <- 0 until blocksNum) {
    blocks(i).io.read := io.read
    blocks(i).io.write := io.write
    blocks(i).io.new_entry := io.new_entry
    if (i != 0) {
      blocks(i).io.right := blocks(i - 1).io.output
    }
    if (i != blocksNum - 1) {
      blocks(i).io.left := blocks(i + 1).io.output
    }
  }
}

object Main extends App {
  println("Hello Chisel World")
  emitVerilog(new ShiftRegister(4, 4, 4))
}
