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
        val token_out = Output(new Token(level + 1)) // 待更新的token
        val cur_pair_in = Input(UInt(pair_width(level).W))
        val next_pair_in = Input(UInt(pair_width(level + 1).W))
        val pair_out = Output(UInt(pair_width(level).W)) // 待更新的pair
    })

    // rename signals
    val op = io.token_in.op
    val position = io.token_in.position
    val push_entry = op.push

    // 当前层读出的两个结点，用于生成更新信号
    val cur_pair = io.cur_pair_in.asTypeOf(Vec(2,new Node(level)))
    val left_node = init_data(cur_pair(0), level) 
    val right_node = init_data(cur_pair(1), level)
    val node = Mux(io.token_in.position(0), right_node, left_node)
    val capacity = node.capacity

    // 左右孩子结点
    val next_pair = io.next_pair_in.asTypeOf(Vec(2,new Node(level + 1)))
    val left_child = init_data(next_pair(0), level + 1)
    val right_child = init_data(next_pair(1), level + 1)

    // generate new pair data
    val update_node = WireInit(Node.default(level)) // 更新当前层的node

    // 注意保持数据一致性(asUInt,asTypeOf)
    io.pair_out := Mux(position(0)
    , Cat(update_node.asUInt, cur_pair(0).asUInt)
    , Cat(cur_pair(1).asUInt, update_node.asUInt))

    // generate new token
    val update_token = WireInit(Token.default(level + 1)) // 需要传递的token
    io.token_out := update_token

    val select_child = WireInit(false.B) // 0 -> lc  1 -> rc
    update_token.op := op 
    update_token.position := Cat(position, select_child.asUInt)

    // 操作状态信号
    val done = WireInit(false.B) // 表示操作是否完成,可参考论文内容
    when (done) {
        update_token.op := Operator.nop
    }.otherwise {}

    // 四个比较器
    val cmp_push_node = push_entry < node.value
    val cmp_push_lc = push_entry < left_child.value
    val cmp_push_rc = push_entry < right_child.value
    val cmp_lc_rc = left_child.value < right_child.value

    val capacity_inc = Mux(capacity === -1.S(capacity_width(level).W).asUInt, capacity, capacity + 1.U)
    val capacity_dec = Mux(capacity === 0.U(capacity_width(level).W), capacity, capacity - 1.U)

    // control
    when (op.pop) { // pop + replace
        done := !left_child.value.existing && !right_child.value.existing

        when (cmp_push_lc) { // replace : 找优先级最高的
            when (cmp_push_rc) {
                update_node.value := Mux(cmp_push_node, node.value, push_entry)
                done := true.B // 已经满足性质
            }.otherwise {
                update_node.value := right_child.value
            }
        }.otherwise { // pop
            update_node.value := Mux(cmp_lc_rc, left_child.value, right_child.value)
        }
        
        // pop则capacity+1；replace则capacity不变
        when (!op.push.existing) {
            update_node.capacity := capacity_inc
        }
        select_child := Mux(cmp_lc_rc, false.B, true.B) // 确定下一位位置，用于生成token

    }.otherwise { // push
        when (cmp_push_node) { 
            update_node.value := push_entry
            update_token.op.push := node.value
            when (!node.value.existing) { // 结点为空直接插入
                done := true.B
            }.otherwise {}

            update_node.capacity := capacity_dec
            select_child := Mux(left_child.capacity === 0.U, true.B, false.B)
        }
    }

}