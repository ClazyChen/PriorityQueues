package ShiftRegisterPQ

import chisel3._;
import chisel3.util._;


// 定义entry结构体
class EntryBlock (dataWidth : Int,rankWidth : Int) extends Bundle {
    val data = UInt(dataWidth.W);                                             
    val rank = UInt(rankWidth.W);                                           
}

// 定义EntryBlock伴生对象，用工厂方法初始化默认entry
object EntryBlock {
    def default(dataWidth : Int,rankWidth : Int) : EntryBlock = {
        val entry = Wire(new EntryBlock(dataWidth,rankWidth));
        entry.data := 0.U(dataWidth.W);                                       
        entry.rank := (-1.S(rankWidth.W)).asUInt;                              
        entry 
    }
}

// 定义ShiftRegisterBlock
// prev:下标较小一侧，nxt：下标较大一侧
class ShiftRegisterBlock(dataWidth : Int,rankWidth : Int) extends Module{

    // 内部io接口
    val io = IO(new Bundle{
        val input_prev = Input(new EntryBlock(dataWidth,rankWidth));        // 从前一个block接收的entry
        val input_nxt = Input(new EntryBlock(dataWidth,rankWidth));         // 从后一个block接收的entry
        val output_prev = Output(new EntryBlock(dataWidth,rankWidth));      // 向前一个block输出的entry
        val output_nxt = Output(new EntryBlock(dataWidth,rankWidth));       // 向后一个block输出的entry
        val insert_cur_index = Input(Bool());                               // 标志位：用于block内部判断当前是否为插入位置
        val cur_input_entry = Input(new EntryBlock(dataWidth,rankWidth));   // 当前新输入的entry
        val cur_output_entry = Output(new EntryBlock(dataWidth,rankWidth)); // 当前block保存的entry，用一个接口供外部访问
        val cmd = Input(Valid(UInt(2.W)));                                  // cmd 表示当前block接受的指令
    });
    
    // 每个shiftregister内部的控制信号
    val DEQ = 0.U(2.W);
    val ENQ = 1.U(2.W);

    // 用来保存entry的寄存器，RegInit要求参数是硬件类型
    val entry_holder = RegInit(EntryBlock.default(dataWidth,rankWidth));

    // initialization output
    io.output_prev := entry_holder;
    io.output_nxt := entry_holder;
    io.cur_output_entry := entry_holder;

    // ShiftRegisterBlock decision logic
    when (io.cmd.valid) {
        switch (io.cmd.bits) {
            is(DEQ) {
                entry_holder := io.input_nxt; // 从下标大的一侧输入 右移
            }
            is (ENQ) {  
                when (io.insert_cur_index) {
                    entry_holder := io.cur_input_entry; // latch
                }           
                .elsewhen(entry_holder.rank > io.cur_input_entry.rank) {
                    entry_holder := io.input_prev; // 从下标小的一侧输入 左移
                }
                .otherwise {
                    // do nothing
                }
            }
        }
    }
    .otherwise{
        // do nothing
    }
}

// 定义top-module，优先队列结构
class PriorityQueueBlock(dataWidth: Int,rankWidth : Int,depth : Int) extends Module {

    // 参数校验
    require(depth > 0, "Depth must be greater than 0");

    // 外部io接口
    val io = IO(new Bundle{
        val read = Input(Bool());                                       // 外部输入信号：read
        val write = Input(Bool());                                      // 外部输入信号：write                             
        val new_entry = Input(new EntryBlock(dataWidth,rankWidth));     // 新到达的entry
        val output_entry = Output(new EntryBlock(dataWidth,rankWidth)); // 最终输出的，优先级最高的entry
    });

    // 对应的控制信号
    val DEQ = 0.U(2.W);
    val ENQ = 1.U(2.W);
    val PULSE = 2.U(2.W); // 表示系统暂停，在这里没有使用到
    val cmd_valid = true.B; // 默认新到达的entry都是有效的
    val cmd = Mux(io.write,ENQ,Mux(io.read,DEQ,PULSE)); // 产生控制信号

    // 生成depth个ShiftRegisterBlock
    val blocks = Seq.fill(depth)(Module(new ShiftRegisterBlock(dataWidth,rankWidth)));

    // 模块连接
    for (i <- 0 until (depth - 1)) {
        blocks(i + 1).io.input_prev <> blocks(i).io.output_nxt;
        blocks(i).io.input_nxt <> blocks(i + 1).io.output_prev;
    }

    for (i <- 0 until depth) {
        blocks(i).io.cmd.valid := cmd_valid;
        blocks(i).io.cmd.bits := cmd;
        blocks(i).io.cur_input_entry := io.new_entry;
    }

    // 给未初始化的sink初始化
    blocks(depth - 1).io.input_nxt := EntryBlock.default(dataWidth,rankWidth);
    blocks(0).io.input_prev := EntryBlock.default(dataWidth,rankWidth);

    // 最终的输出等于0号block向前的输出
    blocks(0).io.output_prev <> io.output_entry;

    // 计算插入位置
    val is_less = WireInit(VecInit(Seq.fill(depth)(false.B)));
    for (i <- 0 until depth) {
        is_less(i) := (blocks(i).io.cur_output_entry.rank > io.new_entry.rank); 
    }
    val reverse_is_less = Wire(UInt(depth.W));
    reverse_is_less := Cat(is_less.reverse); // 数组反转并拼接

    val insert_index = PriorityEncoder(reverse_is_less); // 找到串中第一个1

    // 为每个ShiftRegisterBlock生成标志位
    for (i <- 0 until depth) {
        when (i.U === insert_index) {
            blocks(i).io.insert_cur_index := true.B;
        }
        .otherwise {
            blocks(i).io.insert_cur_index := false.B;
        }
    }  
}


// 入口
object Main extends App {
    println("running chisel app");
    emitVerilog(new PriorityQueueBlock(2,4,4));
}