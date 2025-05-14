package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._
import fpga.mem._


class Cmp(val level : Int) extends Module {
    val io = IO(new Bundle {
        val token_in        = Input(new Token(level))
        val next_token_out  = Output(new Token(level + 1))
        val data_in         = Input(UInt(rw_width(level).W)) 
        val next_data_in    = Input(UInt(rw_width(level + 1).W)) 
        val update_data     = Output(UInt(rw_width(level).W))
    })

    // 简化一下输入的名称，都是wire
    val op = io.token_in.op
    val position = io.token_in.position

    def init_node(node : Node, level : Int) : Node = {
        // 因为优先级全0和existing为0不会同时存在，所以可以用来判断是否初始化过
        val no_init = node.entry.asUInt === 0.U
        Mux(no_init, Node.default(level), node)
    }

    // 读写单位是两个node，所以需要根据position的最后一位确定左右
    val dual_node   = io.data_in.asTypeOf(Vec(2, new Node(level)))
    val node        = init_node( Mux(position(0), dual_node(1), dual_node(0)) , level)
    val entry       = node.entry
    val capacity    = node.capacity

    // 左右孩子
    val next_dual_node = io.next_data_in.asTypeOf(Vec(2, new Node(level + 1)))
    val left_child     = init_node(next_dual_node(0), level + 1)
    val right_child    = init_node(next_dual_node(1), level + 1)

    val update_node    = WireInit(node)
    // 兄弟不变，和更新的节点拼接在一起再输出
    io.update_data :=  Mux(position(0),
        Cat(update_node.asUInt, dual_node(0).asUInt) ,
        Cat(dual_node(1).asUInt, update_node.asUInt))

    val next_token     = Wire(new Token(level + 1))
    io.next_token_out := next_token 

    // next token init
    val select_child     = WireInit(false.B)    // 0 left 1 right
    next_token.position := Cat(position, select_child.asUInt)
    next_token.op       := op                   // unchanged op by default

    val done     = WireInit(false.B)  // false by default
    when (done) {
        next_token.op := Operator.nop
    } 


    // cmp
    val cmp_pl   = op.push          < left_child.entry
    val cmp_pr   = op.push          < right_child.entry
    val cmp_lr   = left_child.entry < right_child.entry
    val cmp_push = op.push          < node.entry

    // 防止capacity溢出
    // 满了再push会弹出右链最大元素，空了pop会返回默认元素
    val capacity_inc = Mux(capacity === -1.S(capacity_width(level).W).asUInt, capacity, capacity + 1.U)
    val capacity_dec = Mux(capacity === 0.U(capacity_width(level).W), capacity, capacity - 1.U)

    when (op.pop) {
        // 按顺序赋值，所以这句放在前面
        done := !left_child.entry.existing && !right_child.entry.existing

        // 不用修改next token op，要么保持原操作，要么done了
        when (cmp_pl) {
            when (cmp_pr) {
                update_node.entry := Mux(cmp_push, node.entry, op.push)
                done := true.B
            } .otherwise {
                update_node.entry := right_child.entry
            }
        } .otherwise {
            update_node.entry := Mux(cmp_lr, left_child.entry, right_child.entry)
        }

        // pop则capacity+1；replace则capacity不变
        when (!op.push.existing) {
            update_node.capacity := capacity_inc
        }

        // pop/replace select child according to compare result
        select_child := ~cmp_lr
    } .otherwise {
        when(cmp_push) {
            update_node.entry := op.push
            next_token.op.push := node.entry 
            when(!node.entry.existing) {
                done := true.B
            }
        }
        // otherwise 保持原操作向下传递

        update_node.capacity := capacity_dec
        // push select child according to capacity
        select_child := left_child.capacity === 0.U
    }
}