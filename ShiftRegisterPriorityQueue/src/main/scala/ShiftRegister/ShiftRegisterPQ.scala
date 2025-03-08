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
        val input_prev = Input(new EntryBlock(dataWidth,rankWidth));
        val input_nxt = Input(new EntryBlock(dataWidth,rankWidth));
        val output_prev = Output(new EntryBlock(dataWidth,rankWidth));
        val output_nxt = Output(new EntryBlock(dataWidth,rankWidth));
        val insert_cur_index = Input(Bool());
        val cur_input_entry = Input(new EntryBlock(dataWidth,rankWidth));
        val cur_output_entry = Output(new EntryBlock(dataWidth,rankWidth));
        val cmd = Input(Valid(UInt(2.W)));
    });
    
    // 每个shiftregister内部的控制信号
    val DEQ = 0.U(2.W);
    val ENQ = 1.U(2.W);

    // 用来保存entry的寄存器
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
                .elsewhen(entry_holder.rank > io.cur_input_entry.rank) { // to-be-fixed
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
        val read = Input(Bool());
        val write = Input(Bool());
        val new_entry = Input(new EntryBlock(dataWidth,rankWidth)); 
        val output_entry = Output(new EntryBlock(dataWidth,rankWidth));
    });

    // 对应的控制信号
    val DEQ = 0.U(2.W);
    val ENQ = 1.U(2.W);
    val PULSE = 2.U(2.W);
    val cmd_valid = true.B;
    val cmd = Mux(io.write,ENQ,Mux(io.read,DEQ,PULSE));

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

    blocks(depth - 1).io.input_nxt := EntryBlock.default(dataWidth,rankWidth);
    blocks(0).io.input_prev := EntryBlock.default(dataWidth,rankWidth);

    blocks(0).io.output_prev <> io.output_entry;

    // 计算插入位置
    val is_less = WireInit(VecInit(Seq.fill(depth)(false.B)));
    for (i <- 0 until depth) {
        is_less(i) := (blocks(i).io.cur_output_entry.rank > io.new_entry.rank); 
    }
    val reverse_is_less = Wire(UInt(depth.W));
    reverse_is_less := Cat(is_less.reverse);

    val insert_index = PriorityEncoder(reverse_is_less);

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