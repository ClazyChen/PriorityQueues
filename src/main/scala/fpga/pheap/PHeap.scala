package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.pheap.Const._

class PHeap extends Module {
  val io = IO(new Bundle {
    val op_in = Input(new Operator)
    val entry_out = Output(new Entry)

    val debug_B = Output(Vec(TreeIndexing.total_node_count, new BNode(count_of_levels)))
  })

  val B = RegInit(VecInit.tabulate(TreeIndexing.total_node_count) { i=>
    val level = TreeIndexing.get_level_from_index(i)
    BNode.default(level)
  })

  io.entry_out := B(0).entry

  val rpus = (1 to count_of_levels).map(level => Module(new RPU(level)))

  rpus(0).io.token_in.operation := io.op_in
  rpus(0).io.token_in.position := 1.U
  rpus(0).io.token_in.value := io.op_in.push

  for(i <- 0 until count_of_levels - 1) {
    rpus(i+1).io.token_in := RegNext(rpus(i).io.token_out)
  }

  def b_read_ports = (1 to count_of_levels).map(level => Wire(new BNode_read_port(level)))
  def b_write_ports = (1 to count_of_levels).map(level => Wire(new BNode_write_port(level)))

  val this_bnode_read_ports = b_read_ports
  val lc_bnode_read_ports = b_read_ports
  val rc_bnode_read_ports = b_read_ports
  val this_bnode_write_ports = b_write_ports

  for(i <- 0 until count_of_levels - 1) {
    val rpu = rpus(i)

    // connect read port and rpu
    this_bnode_read_ports(i).en := rpu.io.this_node_read_en
    this_bnode_read_ports(i).addr := rpu.io.this_node_pos_out
    rpu.io.this_node_value_in := RegNext(this_bnode_read_ports(i).data)

    this_bnode_write_ports(i).en := rpu.io.this_node_write_en
    this_bnode_write_ports(i).addr := rpu.io.this_node_pos_out
    this_bnode_write_ports(i).data := rpu.io.this_node_value_out

    lc_bnode_read_ports(i+1).en := rpu.io.lc_node_read_en
    lc_bnode_read_ports(i+1).addr := rpu.io.lc_node_pos_out
    rpu.io.lc_node_value_in := RegNext(lc_bnode_read_ports(i+1).data)

    rc_bnode_read_ports(i+1).en := rpu.io.rc_node_read_en
    rc_bnode_read_ports(i+1).addr := rpu.io.rc_node_pos_out
    rpu.io.rc_node_value_in := RegNext(rc_bnode_read_ports(i+1).data)
  }

  // initialize the last layer rpu
  val last_num = count_of_levels - 1
  val last_rpu = rpus.last

  this_bnode_read_ports(last_num).en := last_rpu.io.this_node_read_en
  this_bnode_read_ports(last_num).addr := last_rpu.io.this_node_pos_out
  last_rpu.io.this_node_value_in := this_bnode_read_ports(last_num).data

  this_bnode_write_ports(last_num).en := last_rpu.io.this_node_write_en
  this_bnode_write_ports(last_num).addr := last_rpu.io.this_node_pos_out
  this_bnode_write_ports(last_num).data := last_rpu.io.this_node_value_out

  last_rpu.io.lc_node_value_in := BNode.last
  last_rpu.io.rc_node_value_in := BNode.last

  // initialize the first layer rwport
  lc_bnode_read_ports(0).en := false.B
  lc_bnode_read_ports(0).addr := DontCare

  rc_bnode_read_ports(0).en := false.B
  rc_bnode_read_ports(0).addr := DontCare

  for(level <- 1 to count_of_levels) {
    val idx = level - 1
    connect_read_port(this_bnode_read_ports(idx), level)
    connect_write_port(this_bnode_write_ports(idx), level)
    connect_read_port(lc_bnode_read_ports(idx), level)
    connect_read_port(rc_bnode_read_ports(idx), level)
  }

  def connect_read_port(read: BNode_read_port, level: Int): Unit = {
    val base = TreeIndexing.level_start_index(level)
    read.data := Mux(read.en, B(base.U + read.addr), BNode.default(level))
  }
  def connect_write_port(write: BNode_write_port, level: Int): Unit = {
    val base = TreeIndexing.level_start_index(level)
    when(write.en) {
      B(base.U + write.addr) := write.data
    }
  }
  io.debug_B := B
}
