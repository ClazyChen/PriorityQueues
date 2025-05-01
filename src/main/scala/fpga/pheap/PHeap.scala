package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.pheap.Const._

class PHeap extends Module with PriorityQueueTrait {
  val io = IO(new PQIO)

  val left_levels: Seq[LevelMem] = Seq.tabulate(count_of_levels) { idx =>
    val level = idx + 1
    val data_width = 1 << idx
    val node_width = WireDefault(BNode.default(level)).asUInt.getWidth
    val heap_level  = Module(new LevelMem(data_width, node_width))
    heap_level
  }
  val right_levels: Seq[LevelMem] = Seq.tabulate(count_of_levels) { idx =>
    val level = idx + 1
    val data_width = 1 << idx
    val node_width = WireDefault(BNode.default(level)).asUInt.getWidth
    val heap_level  = Module(new LevelMem(data_width, node_width))
    heap_level
  }

  val rpus = (1 to count_of_levels).map(level => Module(new RPU(level)))

  rpus(0).io.token_in.operation := io.op_in
  rpus(0).io.token_in.position := 1.U
  rpus(0).io.token_in.value := io.op_in.push

  io.entry_out := Entry.default

  when(io.op_in.pop) {
    left_levels(0).io.r.en := true.B
    left_levels(0).io.r.addr := 0.U
    io.entry_out := left_levels(0).io.r.data.asTypeOf(new BNode(0)).entry
  }

  for(i <- 0 until count_of_levels - 1) {
    rpus(i+1).io.token_in := RegNext(rpus(i).io.token_out)
  }

  for(i <- 0 until count_of_levels - 1) {
    val level = i+1
    val rpu = rpus(i)

    // connect read port and rpu
    left_levels(i).io.r.en := rpu.io.this_node_read_en
    left_levels(i).io.r.addr := rpu.io.this_node_pos_out
    rpu.io.this_node_value_in := RegNext(left_levels(i).io.r.data.asTypeOf(new BNode(level)))

    left_levels(i).io.w.en := rpu.io.this_node_write_en
    left_levels(i).io.w.addr := rpu.io.this_node_pos_out
    left_levels(i).io.w.data := rpu.io.this_node_value_out.asUInt

    right_levels(i).io.w.en := rpu.io.this_node_write_en
    right_levels(i).io.w.addr := rpu.io.this_node_pos_out
    right_levels(i).io.w.data := rpu.io.this_node_value_out.asUInt

    left_levels(i+1).io.r.en := rpu.io.lc_node_read_en
    left_levels(i+1).io.r.addr := rpu.io.lc_node_pos_out
    rpu.io.lc_node_value_in := RegNext(left_levels(i+1).io.r.data.asTypeOf(new BNode(level+1)))

    right_levels(i+1).io.r.en := rpu.io.rc_node_read_en
    right_levels(i+1).io.r.addr := rpu.io.rc_node_pos_out
    rpu.io.rc_node_value_in := RegNext(right_levels(i+1).io.r.data.asTypeOf(new BNode(level+1)))
  }

  right_levels(0).io.r.en := rpus(0).io.this_node_read_en
  right_levels(0).io.r.addr := rpus(0).io.this_node_pos_out
  rpus(0).io.this_node_value_in := RegNext(left_levels(0).io.r.data.asTypeOf(new BNode(1)))

  // initialize the last layer rpu
  val last_num = count_of_levels - 1
  val last_rpu = rpus.last

  // connect read port and rpu
  left_levels(last_num).io.r.en := last_rpu.io.this_node_read_en
  left_levels(last_num).io.r.addr := last_rpu.io.this_node_pos_out
  last_rpu.io.this_node_value_in := RegNext(left_levels(last_num).io.r.data.asTypeOf(new BNode(last_num)))

  left_levels(last_num).io.w.en := last_rpu.io.this_node_write_en
  left_levels(last_num).io.w.addr := last_rpu.io.this_node_pos_out
  left_levels(last_num).io.w.data := last_rpu.io.this_node_value_out.asUInt

  right_levels(last_num).io.w.en := last_rpu.io.this_node_write_en
  right_levels(last_num).io.w.addr := last_rpu.io.this_node_pos_out
  right_levels(last_num).io.w.data := last_rpu.io.this_node_value_out.asUInt

  last_rpu.io.lc_node_read_en := DontCare
  last_rpu.io.lc_node_pos_out := DontCare
  last_rpu.io.lc_node_value_in := RegNext(BNode.last)

  last_rpu.io.rc_node_read_en := DontCare
  last_rpu.io.rc_node_pos_out := DontCare
  last_rpu.io.rc_node_value_in := RegNext(BNode.last)
}
