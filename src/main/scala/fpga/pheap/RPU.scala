package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._

class RPU(val level: Int, val mem_impl: String) extends Module{
  val io = IO(new Bundle {
    // Receives the token from the previous layer
    val token_in = Input(new TNode(level))
    // Output the token to the previous layer
    val token_out = Output(new TNode(level + 1))
    // TODO：此处对于内存的访问方式可能存在问题
    // access the memory in this layer
    val mem_current_in = Input(new PHeapMem(level, mem_impl))
    // access the memory in next layer
    val mem_next_in = Input(new PHeapMem(level + 1, mem_impl))
  })

  val token_block = RegInit(TNode.default(level))
  token_block := io.token_in
  io.token_out := token_block
  val current_level_mem = Module(new PHeapMem(level, mem_impl))
  bheap_mem_ops.connect(current_level_mem, io.mem_current_in)
  val next_level_mem = Module(new PHeapMem(level + 1, mem_impl))
  bheap_mem_ops.connect(next_level_mem, io.mem_next_in)

  // TODO: 时钟周期
  val i = token_block.position
  val B_block = RegInit(BNode.default(level))
  B_block := current_level_mem.read_block(i, level)

  val lc_bnode = RegInit(BNode.default(level))
  val lc_index = bheap_index_ops.get_lc_pos(i, level)
  lc_bnode := next_level_mem.read_block(lc_index, level + 1)

  val rc_bnode = RegInit(BNode.default(level))
  val rc_index = bheap_index_ops.get_rc_pos(i, level)
  rc_bnode := next_level_mem.read_block(rc_index, level + 1)

  // TODO: 时序控制
  val idle :: enqueue :: dequeue :: Nil = Enum(3)
  val state_reg = RegInit(idle)

  when(state_reg === enqueue) {
    val v = token_block.value
    when(!B_block.entry.existing) {                   // if B[i].active = false
      B_block.entry := v                              // B[i].value <= v; B[i].active <= true;

      io.token_out.operation := Operator.nop          // return done;
      io.token_out.value := DontCare
      io.token_out.position := DontCare
    } .otherwise {                                    // elsif T[j].value > B[i].value
      val enq_cmp_B = v < B_block.entry
      val (next_B_entry, next_T_entry) = entry_ops.swap(enq_cmp_B, v, B_block.entry)   // swap T[j].value, B[i].value;
      B_block.entry := next_B_entry

      val enq_cmp_T = lc_bnode.capacity > 0.U                   // if B[left(i)].capacity > 0: T[j + 1].position <= left(i);
      io.token_out.operation := token_block.operation           // set T[j+1].operation
      io.token_out.value := next_T_entry                        // set T[j+1].value
      io.token_out.position := Mux(enq_cmp_T, lc_index, rc_index)   // else: T(j + 1).position <= right(i);
    }

    B_block.capacity := B_block.capacity - 1.U        // Decrement B[i].capacity;

  }.elsewhen(state_reg === dequeue) {
    when(!lc_bnode.entry.existing && !rc_bnode.entry.existing) {  // if both B[left(i)], B[right(i)] are inactive
      B_block.entry := Entry.default                              // return done;

      io.token_out.operation := Operator.nop
      io.token_out.value := DontCare
      io.token_out.position := DontCare
    } .otherwise {
      val deq_cmp = lc_bnode.entry < rc_bnode.entry                   // Determine the node B[k] with largest value V;
      B_block.entry := Mux(deq_cmp, lc_bnode.entry, rc_bnode.entry)   // B[i].value <= V;
      B_block.capacity := B_block.capacity + 1.U                      // increment B[k].capacity;

      io.token_out.operation := token_block.operation                 // return not-done;
      io.token_out.value := DontCare
      io.token_out.position := Mux(deq_cmp, lc_index, rc_index)       // T[j + 1].position <= k;
    }
    B_block.capacity := B_block.capacity + 1.U
  }

  // write data back to memory
  current_level_mem.write_block(i, B_block)
}

