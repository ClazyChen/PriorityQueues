package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._

class RPU(val level: Int) extends Module {
  val io = IO(new Bundle{
    val token_in = Input(new TNode(level))
    val token_out = Output(new TNode(level+1))

    val this_node_pos_out = Output(UInt(level.W))
    val this_node_read_en = Output(Bool())
    val this_node_value_out = Output(new BNode(level))
    val this_node_write_en = Output(new Bool())

    val this_node_value_in = Input(new BNode(level))

    val lc_node_pos_out = Output(UInt((level+1).W))
    val lc_node_read_en = Output(Bool())

    val lc_node_value_in = Input(new BNode(level+1))

    val rc_node_pos_out = Output(UInt((level+1).W))
    val rc_node_read_en = Output(Bool())

    val rc_node_value_in = Input(new BNode(level+1))
  })

  // initialize the token_block in this layer
  val token_block = RegInit(TNode.default(level))
  val next_token_block = RegInit(TNode.default(level+1))

  val B_block = RegInit(BNode.default(level))
  val lc_pos = RegInit(UInt((level+1).W))
  val lc_block = RegInit(BNode.default(level))
  val rc_pos = RegInit(UInt((level+1).W))
  val rc_block = RegInit(BNode.default(level))

  io.token_out := DontCare

  // initialize the output value
  io.this_node_pos_out := DontCare
  io.this_node_write_en := false.B
  io.this_node_read_en := false.B

  io.lc_node_pos_out := DontCare
  io.lc_node_read_en := false.B

  io.rc_node_pos_out := DontCare
  io.rc_node_read_en := false.B

  val cycle1 :: cycle2 :: cycle3 :: Nil = Enum(3)

  val cycle_state = RegInit(cycle1)

  switch (cycle_state) {
    is(cycle1) {
      // cycle1: read the BNode from this layer and next layer
      token_block := io.token_in

      io.this_node_read_en := true.B
      io.this_node_pos_out := token_block.position - 1.U

      lc_pos := token_block.position - 1.U
      io.lc_node_read_en := true.B
      io.lc_node_pos_out := lc_pos

      rc_pos := token_block.position
      io.rc_node_read_en := true.B
      io.rc_node_pos_out := rc_pos

      cycle_state := cycle2
    }
    is(cycle2) {
      // cycle2:
      // 2.1: access the related value
      B_block := io.this_node_value_in
      lc_block := io.lc_node_value_in
      rc_block := io.rc_node_value_in

      // 2.2: perform push or pop operation
      when(!token_block.operation.pop) {
        // perform push operation
        val v = token_block.value
        when(!B_block.entry.existing) {
          B_block.entry := v            // if B[i].active = false:
                                        // B[i].value <= v; B[i].active = true;
          next_token_block.operation := Operator.nop    // return done
          next_token_block.value := DontCare
          next_token_block.position := DontCare
        } .otherwise {
          val enq_cmp_B = v < B_block.entry   // elsif T[j].value < B[i].value
          val (next_B_entry, next_T_entry) = entry_ops.swap(enq_cmp_B, v, B_block.entry)    // swap T[j].value, B[i].value
          B_block.entry := next_B_entry

          next_token_block.operation := token_block.operation         // return not done
          next_token_block.value := next_T_entry                      // set T[j+1].value
          val enq_cmp_T = lc_block.capacity > 0.U                     // if B[left[i]].capacity > 0: T[j+1].position <= left(i)
          next_token_block.position := Mux(enq_cmp_T, lc_pos, rc_pos)     // else: T[j+1].position <= right(i)
        }
        B_block.capacity := B_block.capacity - 1.U
      }.otherwise {
        // perform pop operation
        when(!lc_block.entry.existing && !rc_block.entry.existing) {    // if both B[left(i)] and B[right(i)] are inactive
          B_block.entry := Entry.default        // return done

          next_token_block.operation := Operator.nop
          next_token_block.value := DontCare
          next_token_block.position := DontCare
        } .otherwise {
          val deq_cmp = lc_block.entry < rc_block.entry         // determine the node B[k] with the largest value v;
          B_block.entry := Mux(deq_cmp, lc_block.entry, rc_block.entry)     // B[i].value <= v;

          next_token_block.operation := token_block.operation     // return not done
          next_token_block.value := DontCare
          next_token_block.position := Mux(deq_cmp, lc_pos, rc_pos)   // T[j+1].position <= k
        }
        B_block.capacity := B_block.capacity + 1.U    // increment B[j].capacity
      }
      // 2.3: update the cycle state
      cycle_state := cycle3
    }
    is(cycle3) {
      // cycle3: write back to the memory
      io.this_node_write_en := true.B
      io.this_node_pos_out := token_block.position
      io.this_node_value_out := B_block
      io.token_out := next_token_block

      cycle_state := cycle1
    }
  }
}
