package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._

class RPU(val level: Int) extends Module {
  val io = IO(new Bundle {
    val token_in = Input(new TNode(level))
    val token_out = Output(new TNode(level + 1))

    val this_node_read_en = Output(Bool())
    val this_node_pos_out = Output(UInt(level.W))
    val this_node_value_in = Input(new BNode(level))

    val this_node_value_out = Output(new BNode(level))
    val this_node_write_en = Output(new Bool())

    val lc_node_read_en = Output(Bool())
    val lc_node_pos_out = Output(UInt((level + 1).W))
    val lc_node_value_in = Input(new BNode(level + 1))

    val rc_node_read_en = Output(Bool())
    val rc_node_pos_out = Output(UInt((level + 1).W))

    val rc_node_value_in = Input(new BNode(level + 1))
  })

  // initialize token block and BNode block in this layer
  val token_block = RegInit(TNode.default(level))
  val next_token_block = RegInit(TNode.default(level))

  val B_block = WireDefault(BNode.default(level))
  val next_B_block = RegInit(BNode.default(level))

  val lc_pos = Reg(UInt((level + 1).W))
  val rc_pos = Reg(UInt((level + 1).W))

  val lc_block = WireDefault(BNode.default(level))
  val rc_block = WireDefault(BNode.default(level))

  val counter = RegInit(0.U)

  // initialize the output value
  io.token_out := TNode.default(level)

  io.this_node_read_en := false.B
  io.this_node_pos_out := 0.U
  io.this_node_write_en := false.B
  io.this_node_value_out := BNode.default(level)

  io.lc_node_pos_out := 0.U
  io.lc_node_read_en := false.B

  io.rc_node_pos_out := 0.U
  io.rc_node_read_en := false.B

  // initialize the state machine
  val cycle1 :: cycle2 :: cycle3 :: Nil = Enum(3)
  val cycle_state = RegInit(cycle1)


  switch(cycle_state) {
    is(cycle1) {
      // cycle1: send read signal to the top level
      token_block := io.token_in
      // 1.1: perform operation when the operator is not nop
      when(io.token_in.operation.pop || io.token_in.value.existing) {
        io.this_node_read_en := true.B
        io.this_node_pos_out := io.token_in.position - 1.U

        lc_pos := io.token_in.position
        rc_pos := io.token_in.position + 1.U

        io.lc_node_read_en := true.B
        io.this_node_pos_out := io.token_in.position - 1.U

        io.rc_node_read_en := true.B
        io.rc_node_pos_out := io.token_in.position
      }
      // 1.2: update the cycle state
      cycle_state := cycle2
    }
    is(cycle2) {
      // cycle2: perform push or pop operation
      // 2.1: access the related value
      B_block := io.this_node_value_in
      lc_block := io.lc_node_value_in
      rc_block := io.rc_node_value_in

      // 2.2: perform push or pop operation
      when(!token_block.operation.pop || token_block.value.existing) {
        // 2.2.1: perform push operation
        val v = token_block.value

        printf(p"[cycle2] level=$level, token.pos=${token_block.position}, token.value.rank=${token_block.value.rank}, lc=$lc_pos, rc=$rc_pos\n")

        when(!B_block.entry.existing) {
          next_B_block.entry := v // if B[i].active = false: B[i].value <= v; B[i].active = true;

          next_token_block.operation := Operator.nop // return done
          next_token_block.value := DontCare
          next_token_block.position := DontCare
        }.otherwise {
          val (next_B_entry, next_T_entry) = B_block.entry.minmax(v) // elsif T[j].value < B[i].value: swap T[j].value, B[i].value

          next_B_block.entry := next_B_entry

          val enq_cmp_T = lc_block.capacity > 0.U                    // if B[left[i]].capacity > 0: T[j+1].position <= left(i)

          next_token_block.operation := token_block.operation        // return not done
          next_token_block.value := next_T_entry                     // set T[j+1].value
          next_token_block.position := Mux(enq_cmp_T, lc_pos, rc_pos)       // else: T[j+1].position <= right(i)
        }

        next_B_block.capacity := B_block.capacity - 1.U // decrement B[j].capacity

      }.elsewhen(token_block.operation.pop) {
        // 2.2.2: perform pop operation
        when(!lc_block.entry.existing && !rc_block.entry.existing) { // if both B[left(i)] and B[right(i)] are inactive
          next_B_block.entry := Entry.default // return done

          next_token_block.operation := Operator.nop
          next_token_block.value := DontCare
          next_token_block.position := DontCare
        }.otherwise {
          val deq_cmp = lc_block.entry < rc_block.entry // determine the node B[k] with the largest value v;
          next_B_block.entry := Mux(deq_cmp, lc_block.entry, rc_block.entry) // B[i].value <= v;

          next_token_block.operation := token_block.operation // return not done
          next_token_block.value := DontCare
          next_token_block.position := Mux(deq_cmp, lc_pos, rc_pos) // T[j+1].position <= k
        }
        next_B_block.capacity := B_block.capacity + 1.U // increment B[j].capacity
      }

      // 2.3: update the cycle state
      cycle_state := cycle3
    }
    is(cycle3) {
      // cycle3: write back to the memory
      when(token_block.operation.pop || token_block.value.existing) {
        io.this_node_write_en := true.B
        io.this_node_pos_out := token_block.position - 1.U
        io.this_node_value_out := next_B_block
        io.token_out := next_token_block
      }
      cycle_state := cycle1
    }
  }
}