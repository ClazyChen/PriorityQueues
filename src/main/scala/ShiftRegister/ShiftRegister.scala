package ShiftRegister
import chisel3._
import chisel3.util._


class PriorityEntry(val priorityWidth: Int, val flowWidth: Int) extends Bundle {
  val priority = UInt(priorityWidth.W)      // 定义元素的优先级
  val flowId   = UInt(flowWidth.W)          // 定义元素的流标号
}

object PriorityEntry {
  def default(priorityWidth: Int, flowWidth: Int): PriorityEntry = {
    val flow_element = Wire(new PriorityEntry(priorityWidth, flowWidth))
    flow_element.priority := -1.S(priorityWidth.W).asUInt // 对元素进行初始化
    flow_element.flowId   := 0.U(flowWidth.W)
    flow_element
  }
}

// 单个优先级队列模块
// 每个模块包含：
//  - 一个保存寄存器用于存储条目；
//  - 比较器：用于比较新条目与当前条目的优先级；
//  - 多路复用器及决策逻辑：决定是锁存新条目、还是从右侧模块移位、或保持当前条目不变。
class ShiftRegisterBlock(val priorityWidth: Int, val flowWidth: Int) extends Module {

  val io = IO(new Bundle{
    val enqueue_Signal  = Input(Bool())      // 入队操作使能信号
    val dequeue_Signal  = Input(Bool())      // 出队操作使能信号

    val enqueue_New_Entry   = Input(new PriorityEntry(priorityWidth, flowWidth))      // 入队模式下，通过全局总线接收的新条目

    val enqueue_Shift_In  = Input(Bool())     // 入队模式下，用于标记此模块右侧模块是否已经接收到新入队元素
    val enqueue_Shift_Out = Output(Bool())    // 入队模式下，用于向左侧模块传递此模块或之前的模块是否已经接收到新入队元素

    val enqueue_Right_Entry = Input(new PriorityEntry(priorityWidth, flowWidth))      // 入队模式下，从右侧相邻模块接收移位数据
    val enqueue_Left_Entry  = Output(new PriorityEntry(priorityWidth, flowWidth))     // 入队模式下，向左侧相邻模块传递移位数据

    val dequeue_Left_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth))      // 出队模式下，从左侧相邻模块接收移位数据
    val dequeue_Right_Entry = Output(new PriorityEntry(priorityWidth, flowWidth))     // 出队模式下，向右侧模块传递移位数据

    val stored_Entry = Output(new PriorityEntry(priorityWidth, flowWidth))    // 输出本Block中存储的数据
  })

  val block_entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth))

  // 对Block内部的输出进行初始化
  io.enqueue_Shift_Out := false.B
  io.enqueue_Left_Entry := block_entry
  io.stored_Entry := block_entry
  io.dequeue_Right_Entry := block_entry

  when(io.enqueue_Signal) {
    // 入队操作的局部决策逻辑：
    when(!io.enqueue_Shift_In && (io.enqueue_New_Entry.priority < block_entry.priority)) {
      // 情况1: 当本模块的右侧模块未接受新入队元素，且本模块元素的优先级小于新入队元素的优先级
      // 将本模块内的元素替换为新入队的元素，并激活enqueue_Shift_Out信号向左侧模块表明已经接收新入队元素
      block_entry := io.enqueue_New_Entry
      io.enqueue_Shift_Out := true.B
    }.elsewhen(io.enqueue_Shift_In) {
      // 情况2: 本模块的右侧模块已经接收新入队的元素，因此直接执行默认的转移过程即可
      block_entry := io.enqueue_Right_Entry
      io.enqueue_Shift_Out := true.B
    }.otherwise {
      // 情况3: 本模块的右侧模块未接收新入队的元素且新入队的元素的优先级小于本模块存储的元素
      // 保持当前条目不变，等价于执行空指令
    }
  } .elsewhen(io.dequeue_Signal) {
    // 出队操作的局部决策逻辑：
    // 本模块来存储来自相邻左侧模块的元素
    block_entry := io.dequeue_Left_Entry
  } .otherwise {
    // 否则保持当前条目不变
  }
}

// 顶层 PriorityQueue 模块，参数化队列容量、数据宽度和优先级宽度
// 此模块实例化多个 Block，并根据 enq/deq 信号将它们按链连接起来
class ShiftRegister(val priorityWidth: Int, val flowWidth: Int, val depth: Int) extends Module {
  val io = IO(new Bundle {
    val enqueue_Signal = Input(Bool())      // 入队信号
    val dequeue_Signal = Input(Bool())      // 出队信号

    val enqueue_Entry = Input(new PriorityEntry(priorityWidth, flowWidth))
    val dequeue_Entry = Output(new PriorityEntry(priorityWidth, flowWidth))

    val block_check = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth)))
  })

  val blocks = VecInit(Seq.fill(depth)(Module(new ShiftRegisterBlock(priorityWidth, flowWidth)).io))

  for(i <- 0 until depth) {
    io.block_check(i) := blocks(i).stored_Entry
  }

  io.dequeue_Entry := blocks(0).stored_Entry
  // 新入队的元素显然不会插入最右侧模块的右侧模块
  // 最左侧的模块显然没有左侧邻接模块
  blocks(0).enqueue_Shift_In    := false.B
  blocks(0).enqueue_Right_Entry := PriorityEntry.default(priorityWidth, flowWidth)
  blocks(depth-1).dequeue_Left_Entry := PriorityEntry.default(priorityWidth, flowWidth)

  // 将入队信号、出队信号和新入队的元素广播到所有模块
  for(i <- 0 until depth) {
    blocks(i).enqueue_Signal := io.enqueue_Signal
    blocks(i).dequeue_Signal := io.dequeue_Signal
    blocks(i).enqueue_New_Entry := io.enqueue_Entry
  }
  // 入队数据的传递链路：
  for(i <- 1 until depth) {
    blocks(i).enqueue_Shift_In := blocks(i-1).enqueue_Shift_Out
    blocks(i).enqueue_Right_Entry := blocks(i-1).enqueue_Left_Entry
  }
  // 出队数据的传递链路：
  for(i <- 0 until depth-1) {
    blocks(i).dequeue_Left_Entry := blocks(i + 1).dequeue_Right_Entry
  }
}


