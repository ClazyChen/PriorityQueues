package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.pheap.Const._

class RPU (val level: Int) extends Module {
  val io = IO(new Bundle {
    val token_in = Input(new TNode(level))
    val token_out = Output(new TNode(level))

    val this_node_pos_out = Output(UInt(level.W))

    val this_node_read_en = Output(Bool())
    val this_node_value_in = Input(new BNode(level))

    val this_node_write_en = Output(new Bool())
    val this_node_value_out = Output(new BNode(level))

    val lc_node_read_en = Output(new Bool())
    val lc_node_pos_out = Output(UInt((level+1).W))
    val lc_node_value_in = Input(new BNode(level+1))

    val rc_node_read_en = Output(new Bool())
    val rc_node_pos_out = Output(UInt((level+1).W))
    val rc_node_value_in = Input(new BNode(level+1))
  })

  // initialize the output value
  io.token_out := TNode.default(level)

  io.this_node_read_en := false.B
  io.this_node_write_en := false.B
  io.this_node_pos_out := 0.U
  io.this_node_value_out := BNode.default(level)

  io.lc_node_read_en := false.B
  io.lc_node_pos_out := 0.U
  io.rc_node_read_en := false.B
  io.rc_node_pos_out := 0.U

  // initialize token block and BNode block in this layer
  val token_block = RegInit(TNode.default(level))
  val next_token_block = RegInit(TNode.default(level))

  val B_block = WireDefault(BNode.default(level))
  val next_B_block = RegInit(BNode.default(level))

  val lc_pos = RegInit(0.U(count_of_levels.W))
  val rc_pos = RegInit(0.U(count_of_levels.W))

  val lc_block = WireDefault(BNode.default(level+1))
  val rc_block = WireDefault(BNode.default(level+1))

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

        val this_lc_pos = TreeIndexing.get_lc_pos(level, io.token_in.position)
        val this_rc_pos = TreeIndexing.get_rc_pos(level, io.token_in.position)

        lc_pos := this_lc_pos
        rc_pos := this_rc_pos

        io.lc_node_read_en := true.B
        io.lc_node_pos_out := this_lc_pos - 1.U

        io.rc_node_read_en := true.B
        io.rc_node_pos_out := this_rc_pos - 1.U

        // 1.1: update the cycle state
        cycle_state := cycle2

      }
    }
    is(cycle2) {
      // cycle2: perform push or pop operation
      // 2.1: access the related value
      B_block := io.this_node_value_in
      lc_block := io.lc_node_value_in
      rc_block := io.rc_node_value_in

      when(token_block.operation.pop  && !token_block.value.existing) {
        // 2.2.1: perform pop operation
        // 2.2.1.1: if both B[left(i)] and B[right(i)] are inactive
        when(!lc_block.entry.existing && !rc_block.entry.existing) {
          next_B_block.entry := Entry.default   // return done

          next_token_block.operation := Operator.nop
          next_token_block.value := DontCare
          next_token_block.position := DontCare
        }.otherwise {
          // 2.2.1.2: B[left(i)].existing || B[right(i)].existing == true
          val cmp = lc_block.entry < rc_block.entry   // determine the node B[k] with the largest value v;
          next_B_block.entry := Mux(cmp, lc_block.entry, rc_block.entry)    // B[i].value <= v;

          next_token_block.operation := token_block.operation     // return not done
          next_token_block.value := token_block.value
          next_token_block.position := Mux(cmp, lc_pos, rc_pos)   // T[j+1].position <= k
        }

        // 2.2.1.3: increment B[j].capacity
        next_B_block.capacity := B_block.capacity + 1.U

      }.elsewhen(!token_block.operation.pop && token_block.value.existing) {
        // 2.2.2: perform push operation
        val v = token_block.value

        when(!B_block.entry.existing) {
          next_B_block.entry := v    // if B[i].active = false: B[i].value <= v; B[i].active = true;

          next_token_block.operation := Operator.nop    // return done
          next_token_block.value := DontCare
          next_token_block.position := DontCare

        }.otherwise {
          val (next_B_entry, next_T_entry) = B_block.entry.minmax(v) // elsif: T[j].value < B[i].value: swap T[j].value, B[i].value

          next_B_block.entry := next_B_entry

          val cmp = lc_block.capacity > 0.U

          next_token_block.operation := token_block.operation // return not done
          next_token_block.value := next_T_entry // set T[j+1].value
          next_token_block.position := Mux(cmp, lc_pos, rc_pos) // else: T[j+1].position <= right(i)
        }

        next_B_block.capacity := B_block.capacity - 1.U

      }

//      printf("===================================================================================\n")
//      printf(p"level=$level, operation=${token_block.operation.pop}, token_pos=${token_block.position}\n")
//      printf(p"token_value=${token_block.value.rank}, B_block_value=${B_block.entry.rank}, B_block_capacity=${B_block.capacity}\n")
//      printf(p"lc_pos=${lc_pos}, lc_capacity=${lc_block.capacity}, lc_value=${lc_block.entry.rank}\n")
//      printf(p"rc_pos=${rc_pos}, rc_capacity=${rc_block.capacity}, rc_value=${rc_block.entry.rank}\n")
//      printf("===================================================================================\n")

      // 2.2.3: update the cycle state
      cycle_state := cycle3

    }
    is(cycle3) {
      // cycle3: write back
      // 3.1: write the new value back to the memory
      when(token_block.operation.pop || token_block.value.existing) {
        io.this_node_write_en := true.B
        io.this_node_pos_out := token_block.position - 1.U
        io.this_node_value_out := next_B_block
        io.token_out := next_token_block
      }

      // 3.2: update the cycle state
      cycle_state := cycle1

    }
  }
}
