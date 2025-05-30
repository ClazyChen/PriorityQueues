package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.mem._
import fpga.Const._
import fpga.pheap.Func._

// cmp inside a RPU
class Cmp (val level : Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new Token(level))
        val token_out = Output(new Token(level + 1)) // token to be updated
        val cur_pair_in = Input(UInt(pair_width(level).W)) 
        val next_pair_in = Input(UInt(pair_width(level + 1).W)) 
        val pair_out = Output(UInt(pair_width(level).W)) // pair data to be updated
    })

    // rename signals
    val op = io.token_in.op
    val position = io.token_in.position
    val push_entry = io.token_in.op.push

    // data from current RPU
    val cur_pair = io.cur_pair_in.asTypeOf(Vec(2, new Node(level)))
    val node = init_data(Mux(position(0), cur_pair(1), cur_pair(0)) , level)
    val entry = node.entry
    val capacity = node.capacity

    // children nodes
    val next_pair = io.next_pair_in.asTypeOf(Vec(2, new Node(level + 1)))
    val left_child = init_data(next_pair(0), level + 1)
    val right_child = init_data(next_pair(1), level + 1)

    // generate new pair data
    // maintain data consistency(asUInt,asTypeOf)
    val update_node = WireInit(node) // update current node
    io.pair_out := Mux(position(0)
    , Cat(update_node.asUInt, cur_pair(0).asUInt) 
    , Cat(cur_pair(1).asUInt, update_node.asUInt))

    // generate new token
    val update_token = Wire(new Token(level + 1))
    io.token_out := update_token 
    val select_child = WireInit(false.B) // 0 -> lc  1 -> rc
    update_token.position := Cat(position, select_child.asUInt)
    update_token.op := op

    // four comparators
    val cmp_push_lc = push_entry < left_child.entry
    val cmp_push_rc = push_entry < right_child.entry
    val cmp_lc_rc = left_child.entry < right_child.entry
    val cmp_push_node = push_entry < node.entry

    // operation state signal
    val done = WireInit(false.B) // operation is done
    when (done) {
        update_token.op := Operator.nop // pass default token
    }.otherwise {}

    // control
    when (op.pop) { // pop + replace
        done := !left_child.entry.existing && !right_child.entry.existing

        when (cmp_push_lc) { // replace : find highest priority
            when (cmp_push_rc) {
                update_node.entry := Mux(cmp_push_node, node.entry, push_entry)
                done := true.B // already satisfy
            } .otherwise {
                update_node.entry := right_child.entry
            }
        } .otherwise { // pop
            update_node.entry := Mux(cmp_lc_rc, left_child.entry, right_child.entry)
        }
        // pop -> capacity+1；replace -> capacity
        when (!push_entry.existing) {
            update_node.capacity := capacity + 1.U // update capacity
        }.otherwise {}
        
        select_child := Mux(cmp_lc_rc, false.B, true.B) // generate next token
    } .otherwise { // push
        when (cmp_push_node) {
            update_node.entry := push_entry
            update_token.op.push := node.entry 
            when(!node.entry.existing) { // node is empty
                done := true.B
            }
        }.otherwise {}

        update_node.capacity := capacity - 1.U // update capacity
        select_child := Mux(left_child.capacity === 0.U, true.B, false.B)
    }
    
}