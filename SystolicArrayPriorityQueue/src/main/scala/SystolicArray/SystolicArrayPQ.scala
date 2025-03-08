package SystolicArrayPQ

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

class SystolicArrayBlock(dataWidth : Int,rankWidth : Int) extends Module {

    // 内部io接口
    val io = IO(new Bundle{
        val input_prev = Input(new EntryBlock(dataWidth,rankWidth));        // 从前一个block接收的entry
        val input_nxt = Input(new EntryBlock(dataWidth,rankWidth));         // 从后一个block接收的entry
        val output_prev = Output(new EntryBlock(dataWidth,rankWidth));      // 向前一个block输出的entry
        val output_nxt = Output(new EntryBlock(dataWidth,rankWidth));       // 向后一个block输出的entry
        val cur_output_entry = Output(new EntryBlock(dataWidth,rankWidth)); // 当前block保存的entry，用一个接口供外部访问
        val cmd = Input(Valid(UInt(2.W)));                                  // cmd 表示当前block接受的指令
    });

    // 声明控制信号
    val DEQ = 0.U(2.W);
    val ENQ = 1.U(2.W);

    // 内部的两个寄存器
    val entry_holder = RegInit(EntryBlock.default(dataWidth,rankWidth));    // 保存当前entry的寄存器
    val low_priority_holder = RegInit(DataBlock.default(dataWidth,rankWidth));       // 记录当前优先级较小entry的寄存器，这个寄存器内的内容用于向下标大的块传递

    // initialization output
    io.output_prev := entry_holder;
    io.output_nxt := low_priority_holder;
    io.cur_output_entry := entry_holder

    // Systolic Array decision block
     when (io.cmd.valid) {
        switch (io.cmd.bits) {
            is(DEQ) {
                entry_holder := io.input_nxt; 
            }
            is (ENQ) {  
               when (low_priority_holder.rank  > io.input_prev.rank) {
                 entry_holder := io.input_prev;
                 tmp_holder := io.input_prev;
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

    // 生成depth个SystolicArrayBlock
    val blocks = Seq.fill(depth)(Module(new SystolicArrayBlock(dataWidth,rankWidth)));

    // 模块连接
    for (i <- 0 until (depth - 1)) {
        blocks(i + 1).io.input_prev <> blocks(i).io.output_nxt;
        blocks(i).io.input_nxt <> blocks(i + 1).io.output_prev;
    }

    // 给未初始化的sink初始化
    blocks(depth - 1).io.input_nxt := EntryBlock.default(dataWidth,rankWidth);
    blocks(0).io.input_prev := EntryBlock.default(dataWidth,rankWidth);

    // 连接0号块与外部接口
    blocks(0).

    // 最终的输出等于0号block向前的输出
    blocks(0).io.output_prev <> io.output_entry;

    // 计算插入位置
    

}


// 入口
object Main extends App {
    println("running chisel app");
    emitVerilog(new PriorityQueueBlock(2,4,4));
}