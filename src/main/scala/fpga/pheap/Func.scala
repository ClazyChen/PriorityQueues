package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node
import fpga.mem


// 定义若干我们会使用到的辅助函数
object Func {
    // 计算capacity位宽
    def capacity_width (cur_level : Int) : Int = {
        count_of_levels - cur_level + 1.U
    }
    // 计算position位宽
    def position_width (cur_level : Int) : Int = {
        cur_level
    }

    // 计算块内索引
    def get_local_index (global_index : Int, cur_level : Int) : Int = {
        global_index - (1 << (cur_level - 1)) + 1.U
    }
    //计算全局索引
    def get_global_index (local_index : Int, cur_level : Int) : Int = {
        val upper_sum = 1.U << (cur_level - 1) - 1.U
        global_index = upper_sum + local_index
        global_index
    }
    // 左孩子全局索引
    def get_lc_global_index (cur_index : Int) : Int = {
        cur_index << 1
    }
    // 右孩子全局索引
    def get_rc_global_index (cur_index : Int) : Int = {
        (cur_index << 1) + 1
    }
    // 左孩子块内索引
    def get_lc_local_index (cur_index : Int, cur_level : Int) : Int = {
        get_local_index(get_lc_global_index(cur_index), cur_level)
    }
    // 右孩子块内索引
    def get_rc_local_index (cur_index : Int, cur_level : Int) : Int = {
        get_local_index(get_rc_global_index(cur_index), cur_level)
    }
    // 判断是否空闲
    def is_empty (node : Node) : Bool = {
        node.value.existing === false.B 
    }
    // 生成操作类型
    def generate_op (token : Token) : State.Type = {
        val state = WireDefault(State.nop)
        when (token.op.push.existing && token.op.pop) {
            state := State.edq
        }.elsewhen (token.op.push.existing) {
            state := State.enq
        }.elsewhen (token.op.pop) {
            state := State.deq
        }.otherwise {}
        state
    } 
    // 判断capacity是否满足capacity > 1
    def is_capacity_valid (node : Node) : Bool = {
        node.capacity >= 1
    }
    // 用三个比较器来计算rank最低(优先级最高)的元素下标
    // def get_lowest_position (signal_0 : Bool, signal_1 : Bool, 
    // signal_2 : Bool, position : UInt) : UInt = {
    //     // get_lowest_position(cmp_lc_rc, cmp_input_lc, cmp_input_rc, token.position)


    // }

    // 把count_of_entries转化成count_of_levels
    def generate_level (current_node_index : Int) : Int = {
        log2Ceil(current_node_index + 1.U)
    }
    
}
