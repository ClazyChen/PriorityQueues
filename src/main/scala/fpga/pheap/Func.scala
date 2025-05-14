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
    // val cmp_lc_rc = io.pair_in.left_node < io.pair_in.right_node
    // val cmp_parent_lc = mem.io.node_out < io.pair_in.left_node
    // val cmp_parent_rc = mem.io.node_out < io.pair_in.right_node
    def get_lowest_position (cmp_lc_rc : Bool, cmp_parent_lc : Bool, 
    cmp_parent_rc : Bool, position : UInt) : UInt = {
        Mux(
        // 情况1：父节点比左右子节点都小 → 直接选父节点
            cmp_parent_lc && cmp_parent_rc,
            position,
            Mux(
                // 情况2：父节点比左子节点小，但不比右子节点小 → 选右子节点
                cmp_parent_lc,
                get_rc_global_index(position)
                Mux(
                    // 情况3：父节点比右子节点小，但不比左子节点小 → 选左子节点
                    cmp_parent_rc,
                    get_lc_global_index(position)
                    // 情况4：父节点不比任何子节点小 → 比较左右子节点，选更小的
                    Mux(cmp_lc_rc, get_lc_global_index(position), get_rc_global_index(position))
                )
            )
        )
    }
    // 把count_of_entries转化成count_of_levels
    def generate_level (current_node_index : Int) : Int = {
        log2Ceil(current_node_index + 1.U)
    }
    
}
