package ShiftRegisterPQ

import chisel3._;
import chisel3.utils._;


// 定义entry结构体
class EntryBlock (address : Int,priority : Int) extends Bundle {
    val address = UInt(address.W);                                      // 指定address位宽
    val priority = UInt(priority.W);                                    // 指定priority位宽
}

// 定义EntryBlock伴生对象，用工厂方法初始化
Object EntryBlock {
    def init_block(address : Int,priority : Int) : EntryBlock {
        val entry_init = Wire(new EntryBlock(address,priority));
        entry_init.address = 0.U;                                       // 设置默认情况下的entry buffer地址为0
        entry_init.priority = 0.U;                                      // 设置默认情况下的entry priority为0
        entry_init
    }
}

// 定义ShiftRegisterBlock，ShiftRegisterBlock内部要求保存entry，同时计算新的逻辑
class ShiftRegisterBlock(address : Int,priority : Int,index : Int) extends Module{
    val io = IO(new Bundle(
        val new_entry = Input(Valid(new EntryBlock(address,priority))); // ShiftRegisterBlock接受的新传入的entry结构
        val read = Input(Bool());                                       // 读信号，效果相当于dequeue
        val write = Input(Bool());                                      // 写信号，效果相当于enqueue
        val left_reg_entry = Input(new EntryBlock(address,priority));   // 当前block向左传输的entry
        val right_reg_entry = Input(new EntryBlock(address,priority));  // 当前block向右传输的entry
        val output_entry = Output(new EntryBlock(address,priority));    // 输出entry
    ));

    // 用一个寄存器来保存ShiftRegisterBlock内部的entry结构，调用前面的初始化方法
    val entry_holder = RegInit(EntryBlock.init_block(address,priority));

    // 初始化上述定义的若干控制信号
    io.read = false.B;
    io.write = false.B;

    // write实现，效果相当于enqueue
    when (io.write & !io.read) {

    }
    // read实现，效果相当于dequeue
    when (io.read & !io.write) {

    }


}

// 定义top-module，优先队列结构
class PriorityQueueBlock() {

}