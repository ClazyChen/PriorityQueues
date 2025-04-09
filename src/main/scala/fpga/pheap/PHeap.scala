package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._


// 如果满了之后push，会自然丢弃右链最大的元素
class PHeap extends Module with PriorityQueueTrait {
    val io = IO(new PQIO)

    // the token array in the pheap
    val token_array = Seq.fill(level)(Module(new Token))

    // the rank processing unit in the pheap
    // 想法：
    //      看起来一层一个RPU，但由于某层执行时相邻层必然nop，
    //      所以只需要(level + 1) / 2个RPU就行了？
    //      即RPUi负责处理第2i - 1, 2i层
    val RPUs = Seq.fill(level)(Module(new RPU))

    // 创建SRAM并连线

    // 状态记录（这里的讨论先不考虑上面的共有RPU想法）
    // 第一种想法：
    //      全局同步状态：read、cmp、write、nop1、nop2、nop3六个状态轮着走
    //      奇数level的RPU状态一致，偶数level的RPU状态一致，在这里定义变量传进去
    //      刚开机是nop，等检测到输入有信号时，下个时钟沿将奇数组状态转为read，
    //      偶数组状态转为nop1，随后轮转
    //      需要使用者严格相隔6个周期输入
    // 第二种想法：
    //      状态存在RPU上，靠前面的RPU推动后面的RPU
    //      只要使用者至少相隔6个周期输入就行
}