package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Func._
import fpga.pheap.Node._

// cmp inside a RPU
class Cmp (val level : Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new Token(level))
        val node_in = Input(new Node(level))
        val next_pair_in = Input(new Pair(level + 1))
        val cur_pair_in = Input(new Pair(level))
        val token_out = Output(new Token(level + 1)) // 待更新的token
        val pair_out = Output(UInt(Pair.getWidth(level).W)) // 待更新的pair
    })

    // rename signals
    val token = io.token_in
    val op = token.op
    val position = token.position
    val push_entry = op.push
    // 这里是我们要操作的node
    val node = init_data(io.node_in, level) 
    val capacity = node.capacity
    // 当前层读出的两个结点，用于生成更新信号
    val left_node = init_data(io.cur_pair_in.left_node, level) 
    val right_node = init_data(io.cur_pair_in.right_node, level)
    // 左右孩子结点
    val left_child = init_data(io.next_pair_in.left_node, level + 1)
    val right_child = init_data(io.next_pair_in.right_node, level + 1)

    // 四个比较器
    val cmp_push_node = push_entry < node.value
    val cmp_push_lc = push_entry < left_child.value
    val cmp_push_rc = push_entry < right_child.value
    val cmp_lc_rc = left_child.value < right_child.value

    // 辅助信号，都是wire类型
    val update_node = WireInit(Node.default(level)) // 更新当前层的node
    val update_token = WireInit(Token.default(level)) // 需要传递的token
    val select_child = WireInit(false.B) // 0 -> lc  1 -> rc

    // 生成需要更新的pair
    // 注意保持数据一致性(asUInt,asTypeOf)
    io.pair_out := Mux(position(0)
    , Cat(update_node.asUInt, left_node.asUInt)
    , Cat(right_node.asUInt, update_node.asUInt))

    // 生成需要更新的token
    update_token.op := op 
    update_token.position := Cat(position, select_child.asUInt)

    val done = WireInit(false.B) // 表示操作是否完成,可参考论文内容
    when (done) {
        update_token.op := Operator.nop
    }.otherwise {}

    // 初始化
    io.token_out := update_token

    // control
    when (op.pop) { // pop + replace
        done := !left_child.value.existing && !right_child.value.existing

        when (cmp_push_lc) { // replace : 找优先级最高的
            when (cmp_push_rc) {
                update_node.value := Mux(cmp_push_node, node.value, push_entry)
                done := true.B // 已经满足性质
            }.otherwise {
                update_node.value := Mux(cmp_lc_rc, left_child.value, right_child.value)
            }
        }.otherwise { // pop
            update_node.value := Mux(cmp_lc_rc, left_child.value, right_child.value)
        }
        
        update_node.capacity := Mux(push_entry.existing, capacity ,capacity + 1.U) // 更新capacity
        select_child := Mux(cmp_lc_rc, false.B, true.B) // 确定下一位位置，用于生成token

    }.otherwise { // push
        when (cmp_push_node) { 
            update_node.value := push_entry
            update_token.op.push := node.value
            when (!node.value.existing) { // 结点为空直接插入
                done := true.B
            }.otherwise {}

            update_node.capacity := capacity - 1.U
            select_child := Mux(left_child.capacity === 0.U, true.B, false.B)
        }
    }

}