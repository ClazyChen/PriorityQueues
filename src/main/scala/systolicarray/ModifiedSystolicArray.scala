package systolicarray

import chisel3._
import chisel3.util._

class ShiftRegisterBlock(val priorityWidth: Int, val flowWidth: Int, val subWidth: Int) extends Module {
  val io = IO(new Bundle{
    val new_Entry      = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))               // 入队时，通过全局总线接收的新条目
    val enqueue_Signal = Input(Bool())                                                              // 入队操作使能信号（当 enq 有效且 deq 未激活时进入入队逻辑）
    val dequeue_Signal = Input(Bool())                                                              // 出队操作使能信号（当deq 有效时进入出队逻辑）

    val shift_Flag_In  = Input(Bool())                                                              // 入队模式下，从右侧相邻模块接收标志
    val shift_Flag_Out = Output(Bool())                                                             // 入队模式下的移位标志，若本模块发生了新元素的入队或右侧模块的已经接收了新元素，则输出为 true
    val enqueue_Right_Entry = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))          // 入队模式下，从右侧相邻模块接收移位数据及其标志
    val enqueue_Left_Entry  = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))         // 入队模式下，将本模块需要右移的与元素通过此端口移位给左侧模块

    val dequeue_Left_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))          // 出队模式下，从左侧相邻模块获取数据（用于右移）
    val dequeue_Right_Entry = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))         // 出队模式下，此输出用于传递给右侧模块

    val stored_entry = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))                // 出队模式下，此输出用于产生最终的输出结果
  })

  val entry = RegInit(PriorityEntry.default(priorityWidth, flowWidth, subWidth))

  io.stored_entry := entry
  io.shift_Flag_Out := false.B
  io.dequeue_Right_Entry  := entry
  io.enqueue_Left_Entry := entry

  when(io.enqueue_Signal) {
    // 入队操作：局部决策逻辑
    // 只有当来自右侧模块未发生移位且新条目有效时，
    // 若新条目的优先级高于当前条目，则本模块锁存新条目，并将原条目作为移位数据输出给左侧模块。
    when(!io.shift_Flag_In && (io.new_Entry.priority <= entry.priority)) {
      io.shift_Flag_Out := true.B
      when(io.new_Entry.priority < entry.priority) {
        // 当新条目的优先级大于原条目时
        // 锁存新条目，同时输出原条目实现向左侧模块移位
        entry := io.new_Entry
      }.otherwise {
        // 当新条目的优先级等于原条目时
        // 比较二者的下标并决定移位元素
        when(io.new_Entry.subscript > entry.subscript) {
          // 当新条目的下标更大时:
          // 新条目作为左移条目，原条目保持不变
          io.enqueue_Left_Entry := io.new_Entry
        }.otherwise {
          // 当原条目的下标更大时
          // 原条目作为左移条目，新条目成为entry
          entry := io.new_Entry
        }
      }
    }.elsewhen(io.shift_Flag_In) {
      // 如果本模块的右侧模块已锁存新条目，则本模块从右侧接收移位数据，实现条目向左移动
      io.shift_Flag_Out := true.B
      entry := io.enqueue_Right_Entry
    } .otherwise {
      // 否则保持当前条目不变
    }
  } .elsewhen(io.dequeue_Signal) {
    // 出队操作：模块0的条目将被读取，而其他模块则从左侧模块接收数据，实现所有条目向右移动
    entry := io.dequeue_Left_Entry
  } .otherwise {
    // 否则保持当前条目不变
  }
}

class ModifiedSystolicArrayBlock(val priorityWidth: Int, val flowWidth: Int, val depth: Int, val subWidth: Int) extends Module {
  val io = IO(new Bundle {
    val block_enqueue_Signal = Input(Bool())
    val block_dequeue_Signal = Input(Bool())

    val block_enqueue_entry = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))
    val block_dequeue_entry = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))

    val data_check = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth, subWidth)))

    // 用于向左侧模块传递溢出的元素，在模块0～length-2中，应与其左侧模块的block_enqueue_entry相连接
    val enqueue_Shift_Left_Entry  = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))
    // 在出队模式下，用于从左侧模块获取到邻接的元素，在模块0～length-2中，应与其左侧的block_dequeue_
    val dequeue_Shift_Left_Entry  = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))

    val block_output_enqueue_Signal = Output(Bool())      // 用于向左侧的模块传递入队信号
    val block_output_dequeue_Signal = Output(Bool())      // 用于向左侧的模块传递出队信号
  })

  // 实例化一组ShiftBlocks
  // 维护entry_counter用于记录当前block中的元素个数
  val shiftblocks = VecInit(Seq.fill(depth)(Module(new ShiftRegisterBlock(priorityWidth, flowWidth, subWidth)).io))
  val entry_counter = RegInit(0.U(depth.W))

  for(i <- 0 until depth) {
    io.data_check(i) := shiftblocks(i).stored_entry
  }
  io.block_output_enqueue_Signal := io.block_enqueue_Signal
  io.block_output_dequeue_Signal := io.block_dequeue_Signal
  // ---------------------------------------------------------------------------------------
  // 入队逻辑块：
  // 新入队的元素显然不会插入最右侧Block的右侧
  // 最右侧的Block的右侧显然没有Block
  shiftblocks(0).shift_Flag_In := false.B
  shiftblocks(0).enqueue_Right_Entry  := PriorityEntry.default(priorityWidth, flowWidth, subWidth)
  // 模块 i+1 的移位输入连接来自模块 i 的移位输出及标志
  for(i <- 0 until depth-1) {
    shiftblocks(i+1).enqueue_Right_Entry := shiftblocks(i).enqueue_Left_Entry
    shiftblocks(i+1).shift_Flag_In := shiftblocks(i).shift_Flag_Out
  }
  // 默认情况下，不执行任何操作
  for(i <- 0 until depth) {
    shiftblocks(i).new_Entry := PriorityEntry.default(priorityWidth, flowWidth, subWidth)
    shiftblocks(i).enqueue_Signal := false.B
  }
  // 默认情况下，入队操作中产生溢出的元素为最左侧的元素
  io.enqueue_Shift_Left_Entry := shiftblocks(depth-1).stored_entry
  // ---------------------------------------------------------------------------------------

  // ---------------------------------------------------------------------------------------
  // 出队逻辑块
  // 保证所有block都可以接收到出队信号
  for(i <- 0 until depth) {
    shiftblocks(i).dequeue_Signal := io.block_dequeue_Signal && !io.block_enqueue_Signal
  }
  // 出队的元素默认为最右侧block中存储的元素
  io.block_dequeue_entry := shiftblocks(0).stored_entry
  // 最左侧没有左侧邻接模块，故将其 deq_Left_Entry 置为默认值
  // 每次执行出队操作时，block都会其左边的block获取元素  // 每次执行出队操作时，block都会其左边的block获取元素
  for(i <- 0 until depth-1) {
    shiftblocks(i).dequeue_Left_Entry := shiftblocks(i+1).dequeue_Right_Entry
  }
  shiftblocks(depth-1).dequeue_Left_Entry := io.dequeue_Shift_Left_Entry
  // ---------------------------------------------------------------------------------------

  when(io.block_enqueue_Signal) {
    when(entry_counter < depth.U) {
      // 当前模块未满时，溢出元素默认为最左侧的元素
      // 新入队的元素有机会插入该模块的任意一个block中
      for(i <- 0 until depth) {
        shiftblocks(i).new_Entry := io.block_enqueue_entry
        shiftblocks(i).enqueue_Signal := io.block_enqueue_Signal
      }
      io.enqueue_Shift_Left_Entry := shiftblocks(depth-1).stored_entry
      entry_counter := entry_counter + 1.U
    } .elsewhen(entry_counter === depth.U) {
      // 当前模块已满时，有以下两种情况：
      // 情况1: 如果新入队的元素无法插入到当前模块，则溢出元素为新入队的元素
      // 情况2: 如果新入队的元素有机会插入该模块，则溢出元素为最左侧的元素
      when(io.block_enqueue_entry.priority >= shiftblocks(depth-1).stored_entry.priority) {
        // 当情况1发生时，模块不执行入队操作，将新入队的元素直接赋给溢出元素
        io.enqueue_Shift_Left_Entry := io.block_enqueue_entry
      } .elsewhen(io.block_enqueue_entry.priority < shiftblocks(depth-1).stored_entry.priority) {
        // 当情况2发生时，模块执行入队操作，并将最左侧block的输出元素赋给溢出元素
        for(i <- 0 until depth) {
          shiftblocks(i).new_Entry := io.block_enqueue_entry
          shiftblocks(i).enqueue_Signal := io.block_enqueue_Signal
        }
        io.enqueue_Shift_Left_Entry := shiftblocks(depth-1).enqueue_Left_Entry
      }
    }
  } .elsewhen(io.block_dequeue_Signal) {
    // 出队模式下，各个block从左侧的接口获取数据从而实现整体右移
    // 需要考虑边界问题，如果该模块的左侧模块内有值，则需从左侧模块提供的接口获取
    // 在模块1～length-1中，默认出队的元素就是右移的溢出元素
    when(entry_counter >= 1.U) {
      entry_counter := entry_counter - 1.U
    }.elsewhen(entry_counter === 0.U) {
      // 如果此时模块为空，则执行空操作
    }
  }
}

class ModifiedSystolicArrayPriorityQueue(val priorityWidth: Int, val flowWidth: Int, val depth: Int, val length: Int, val subWidth: Int) extends Module{
  val io = IO(new Bundle {
    val enqueue_Signal = Input(Bool())
    val dequeue_Signal = Input(Bool())

    val enqueue_Entry = Input(new PriorityEntry(priorityWidth, flowWidth, subWidth))
    val dequeue_Entry = Output(new PriorityEntry(priorityWidth, flowWidth, subWidth))
  })

  val modules = VecInit(Seq.fill(length)(Module(new ModifiedSystolicArrayBlock(priorityWidth, flowWidth, depth, subWidth)).io))

//  val data_check = Output(Vec(depth, new PriorityEntry(priorityWidth, flowWidth)))
  modules(0).block_enqueue_Signal := io.enqueue_Signal
  modules(0).block_dequeue_Signal := io.dequeue_Signal
  modules(0).block_enqueue_entry  := io.enqueue_Entry
  io.dequeue_Entry := modules(0).block_dequeue_entry

  for(i <- 1 until length) {
    // 对于模块1～length-1来说：入队模式下入队的元素为右侧模块溢出的元素
    // 对于模块0～length-2来说：出队模式下补入的元素为左侧模块出队的元素
    modules(i).block_enqueue_entry := modules(i-1).enqueue_Shift_Left_Entry
    modules(i-1).dequeue_Shift_Left_Entry := modules(i).block_dequeue_entry
  }
  modules(length-1).dequeue_Shift_Left_Entry := PriorityEntry.default(priorityWidth, flowWidth, subWidth)

  for(i <- 1 until length) {
    modules(i).block_enqueue_Signal := modules(i-1).block_output_enqueue_Signal
    modules(i).block_dequeue_Signal := modules(i-1).block_output_dequeue_Signal
  }
}
