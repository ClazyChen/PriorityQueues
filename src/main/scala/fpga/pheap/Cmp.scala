package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.pheap.Const._
import fpga.mem._


class Cmp(val level : Int) extends Module {
    val io = IO(new Bundle {
        val enable_in            = Input(Bool())
        val enable_out           = Output(Bool())
        val token_in             = Input(new Token(level))
        val next_token_out       = Output(new Token(level + 1))
        val dual_node_in         = Input(UInt(rw_width(level).W)) 
        val next_dual_node_in    = Input(UInt(rw_width(level + 1).W)) 
        val update_dual_node_out = Output(UInt(rw_width(level).W))
        val done_out             = Output(Bool()) 
        val entry_out            = if (level == 1) Some(Output(new Entry)) else None
    })

    // 延迟一拍，启动下个模块
    val enable = RegNext(io.enable_in) 
    io.enable_out := enable 

    // 简化一下输入的名称，都是wire
    val op = io.token_in.op
    val position = io.token_in.position

    def init_node(node : Node, level : Int) : Node = {
        // 因为优先级全0和existing为0不会同时存在，所以可以用来判断是否初始化过
        val no_init = node.entry.asUInt === 0.U
        Mux(no_init, Node.default(level), node)
    }

    // 读写单位是两个node，所以需要根据position的最后一位确定左右
    val dual_node   = io.dual_node_in.asTypeOf(Vec(2, new Node(level)))
    val node        = init_node( Mux(position(0), dual_node(0), dual_node(1)) , level)
    val entry       = node.entry
    val capacity    = node.capacity
    io.entry_out match {
        case Some(entry_out) => entry_out := entry
        case None            =>
    }

    // 左右孩子
    val next_dual_node = io.next_dual_node_in.asTypeOf(Vec(2, new Node(level + 1)))
    val left_child     = init_node(next_dual_node(0), level + 1)
    val right_child    = init_node(next_dual_node(1), level + 1)

    // 输出
    val update_node          = WireInit(Node.default(level))
    val update_dual_node_reg = RegNext(Mux(position(0),
        Cat(update_node.asUInt, dual_node(1).asUInt) ,
        Cat(dual_node(0).asUInt, update_node.asUInt) ))  // 和兄弟拼接在一起
    io.update_dual_node_out := update_dual_node_reg

    val next_token     = Wire(new Token(level + 1))
    val next_token_reg = RegNext(next_token) // 第三个周期再传给下层RPU
    io.next_token_out := next_token_reg 

    // next token init
    val select_child     = WireInit(false.B)    // 0 left 1 right
    next_token.position := Cat(position, select_child.asUInt)
    next_token.op       := op                   // unchanged op by default

    val done     = WireInit(false.B)  // false by default
    val done_reg = RegNext(done)  // 下个周期传给write，write根据done决定enable_out
    io.done_out := done_reg 

    // cmp
    // p = push, l = left child, r = right child
    val cmp_pl   = op.push          < left_child.entry
    val cmp_pr   = op.push          < right_child.entry
    val cmp_lr   = left_child.entry < right_child.entry
    val cmp_push = op.push          < node.entry

    val capacity_inc = node.capacity + 1.U
    val capacity_dec = node.capacity - 1.U

    // pop = replace Entry.default
    when (op.pop) {
        // 按顺序赋值，95行改done，所以这句放在前面
        done := !left_child.entry.existing && !right_child.entry.existing

        // 不用修改next token op，要么保持原操作，要么done了
        when (cmp_pl) {
            when (cmp_pr) {
                update_node.entry := op.push
                done := true.B
            } .otherwise {
                update_node.entry := right_child.entry
            }
        } .otherwise {
            update_node.entry := Mux(cmp_lr, left_child.entry, right_child.entry)
        }

        // 父节点肯定是active的，所以只需判断op即可
        // TODO 如果PQ空的时候，执行一个pop操作咋办
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
                // TODO capacity可以像BBQ介绍的那样优化
                // TODO 溢出如何处理，capacity不变
                update_node.capacity := capacity_dec
                done := true.B
            }
        }
        // otherwise 保持原操作向下传递

        // push select child according to capacity
        select_child := left_child.capacity === 0.U
    }
}