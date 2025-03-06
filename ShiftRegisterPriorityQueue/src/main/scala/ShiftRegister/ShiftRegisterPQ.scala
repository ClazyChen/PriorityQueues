package ShiftRegisterPQ

import chisel3._;
import chisel3.util._;


// 定义entry结构体
class EntryBlock (addressWidth : Int,priorityWidth : Int) extends Bundle {
    val address = UInt(addressWidth.W);                                             // 指定address位宽
    val priority = UInt(priorityWidth.W);                                           // 指定priority位宽
}

// 定义EntryBlock伴生对象，用工厂方法初始化默认entry
object EntryBlock {
    def init_block(addressWidth : Int,priorityWidth : Int) : EntryBlock = {
        val entry = Wire(new EntryBlock(addressWidth,priorityWidth));
        entry.address := 0.U(addressWidth.W);                                       // 设置默认情况下的entry buffer地址为0
        entry.priority := 0.U(priorityWidth.W);                                     // 设置默认情况下的entry priority为0
        entry
    }
}

// 定义ShiftRegisterBlock，ShiftRegisterBlock内部要求保存entry，同时利用标志位计算新的逻辑
class ShiftRegisterBlock(addressWidth : Int,priorityWidth : Int,index : Int) extends Module{
    val io = IO(new Bundle{
        val current_entry = Output(new EntryBlock(addressWidth,priorityWidth));     // 仅用于read输出
        val input_entry = Input(new EntryBlock(addressWidth,priorityWidth));        // ShiftRegisterBlock接受的新传入的entry
        val right_shift = Input(Bool());                                            // 右移使能信号
        val right_shift_entry = Output(new EntryBlock(addressWidth,priorityWidth)); // 向右侧的block输出的entry
        val left_shift = Input(Bool());                                             // 左移使能信号
        val left_shift_entry = Output(new EntryBlock(addressWidth,priorityWidth));  // 向左侧的block输出的entry
    });
    // 用来保存当前block中的entry
    val entry_holder = RegInit(EntryBlock.init_block(addressWidth,priorityWidth));
    // initialization
    io.current_entry := entry_holder;
    io.right_shift := false.B;
    io.right_shift_entry := entry_holder;
    io.left_shift := false.B;
    io.left_shift_entry := entry_holder;

    // 左移使能触发左移逻辑
    when (io.left_shift && !io.right_shift) {
        // 把当前entry输出到左移端口中
        io.left_shift_entry := entry_holder;
    }
    // 右移使能触发右移逻辑
    .elsewhen (io.right_shift && !io.left_shift) {
        // 把当前entry输入到右移端口中
        io.right_shift_entry := entry_holder;
    }
    .otherwise{
        // empty
    }
}

// 定义top-module，优先队列结构
class PriorityQueueBlock(addressWidth: Int,priorityWidth : Int,depth : Int) extends Module {
    // 参数校验
    require(depth >= 1, "Depth must be greater than 0");

    val io = IO(new Bundle{
        val read = Input(Bool());
        val write = Input(Bool());
        val new_entry = Input(Valid(new EntryBlock(addressWidth,priorityWidth)));   // 用于外部到达的new_entry入队
        val output_entry = Output(new EntryBlock(addressWidth,priorityWidth));
    });
    // 生成ShiftRegisterBlock子模块实例
    val regs = Seq.tabulate(depth) { i =>
        val reg = Module(new ShiftRegisterBlock(
            addressWidth,
            priorityWidth,
            i
        ));
        reg.io.right_shift := false.B;                                              // 在父模块的子模块中对input端口赋值，全部初始化为false
        reg.io.left_shift := false.B;
        reg
    }
    // 定义write逻辑，效果等效于enqueue
    // 要实现的效果：插入block之后右侧（index较小侧）的priority均大于此项，而左侧（index较大侧）的priority均小于此项
    when (io.write && !io.read) {
        // 判断插入位置
        for (i <- (depth - 1) to 0 by -1) {
            when (regs(i).entry_holder.priority < io.new_entry.bits.priority) {
                regs(i).io.left_shift := true.B;
            }
            .otherwise {
                // do nothing
            }
        }
        for (i <- (depth - 1) to 0 by -1) {
            when (regs(i).io.left_shift) {
                regs(i).io.input_entry := regs(i - 1).io.left_shift_entry;
                regs(i).entry_holder := regs(i).io.input_entry;
            }
            .otherwise {
                // empty
            }
        }
        // 插入新entry
        for (i <- (depth - 1) to 0 by -1) {
            when (!regs(i).io.left_shift) {
                regs(i).io.input_entry := io.new_entry;
                regs(i).entry_holder := regs(i).io.input_entry;
            }
            .otherwise {
                // empty
            }
        }
    }
    // 定义read逻辑，效果等效于dequeue
    .elsewhen (io.read && !io.write) {
        for (i <- 0 until depth) {
            when (i.U =/= 0.U) {
                regs(i).io.right_shift := true.B;                                   // 除了0号所有的ShiftRegisterBlock都设置右移使能
            }
            .otherwise{
                // empty
            }
        }
        regs(0).io.current_entry := regs(0).entry_holder;
        io.output_entry := regs(0).io.current_entry;
        for (i <- 0 until depth - 1) {
            regs(i).io.input_entry := regs(i + 1).io.right_shift_entry;
            regs(i).entry_holder := regs(i).io.input_entry;
        }
        regs(depth - 1).entry_holder := EntryBlock.init_block(addressWidth, priorityWidth); // block index为depth - 1处添加一个默认entry
    }
    .otherwise(
        // empty 
    )
}

// 入口
object Main extends App {
    println("running app");
    emitVerilog(new PriorityQueueBlock(6,10,8));
}