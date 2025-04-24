package fpga.pheap

import chisel3._
import chisel3.util._
import fpga._
import fpga.pheap.Const._


class PHeap extends Module {
  val io = IO(new Bundle {
    val op_in = new Operator
    val entry_in = new Entry
    val entry_out = new Entry
  })

  /*
            1               level1  T[1]  rpu(0)
        2       3           level2  T[2]  rpu(1)
     4   5     6   7        level3  T[3]  rpu(2)
   8 9 10 11 12 13 14 15    level4  T[4]  rpu(3)

Index :   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14
Level :   1   2   2   3   3   3   3   4   4   4   4   4   4   4   4
Node :    1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
  */

  val B = RegInit(VecInit.tabulate(TreeIndexing.total_node_count) { i =>
    val level = TreeIndexing.get_level_from_index(i)
    BNode.default(level)
  })

  val rpus = (1 to count_of_levels).map(level => Module(new RPU(level)))

  rpus(0).io.token_in.operation := io.op_in
  rpus(0).io.token_in.position := 1.U
  rpus(0).io.token_in.value := io.entry_in

  io.entry_out := B(0)

  for(i <- 0 until count_of_levels ) {
    rpus(i+1).io.token_in := rpus(i).io.token_out
  }

  // 分层端口
  def mkReadPorts = (1 to count_of_levels).map(level => Wire(new BNodeReadPort(level)))
  def mkWritePorts = (1 to count_of_levels).map(level => Wire(new BNodeWritePort(level)))

  val b_this_read_ports  = mkReadPorts
  val b_lc_read_ports    = mkReadPorts
  val b_rc_read_ports    = mkReadPorts
  val b_this_write_ports = mkWritePorts

  // 连接每层 RPU 到接口
  for (i <- 0 until count_of_levels - 1) {
    val rpu = rpus(i)
    // 当前层 this_node
    b_this_read_ports(i).addr := rpu.io.this_node_pos_out
    b_this_read_ports(i).en   := rpu.io.this_node_read_en
    rpu.io.this_node_value_in := b_this_read_ports(i).data

    b_this_write_ports(i).addr := rpu.io.this_node_pos_out
    b_this_write_ports(i).en   := rpu.io.this_node_write_en
    b_this_write_ports(i).data := rpu.io.this_node_value_out

    // 下一层子节点
    val j = i + 1
    b_lc_read_ports(j).addr := rpu.io.lc_node_pos_out
    b_lc_read_ports(j).en   := rpu.io.lc_node_read_en
    rpu.io.lc_node_value_in := b_lc_read_ports(j).data

    b_rc_read_ports(j).addr := rpu.io.rc_node_pos_out
    b_rc_read_ports(j).en   := rpu.io.rc_node_read_en
    rpu.io.rc_node_value_in := b_rc_read_ports(j).data
  }

  // 最后一层 RPU 特殊处理
  val last = count_of_levels - 1
  val last_rpu = rpus.last

  b_this_read_ports(last).addr := last_rpu.io.this_node_pos_out
  b_this_read_ports(last).en   := last_rpu.io.this_node_read_en
  last_rpu.io.this_node_value_in := b_this_read_ports(last).data

  b_this_write_ports(last).addr := last_rpu.io.this_node_pos_out
  b_this_write_ports(last).en   := last_rpu.io.this_node_write_en
  b_this_write_ports(last).data := last_rpu.io.this_node_value_out

  last_rpu.io.lc_node_value_in := BNode.default(count_of_levels + 1)
  last_rpu.io.rc_node_value_in := BNode.default(count_of_levels + 1)

  // 通用函数：根据端口读写 B 数组
  def connect_read_port(read: BNodeReadPort, level: Int): Unit = {
    val base = TreeIndexing.level_start_index(level)
    read.data := Mux(read.en, B(base.U + read.addr), BNode.default(level))
  }

  def connect_write_port(write: BNodeWritePort, level: Int): Unit = {
    val base = TreeIndexing.level_start_index(level)
    when(write.en) {
      B(base.U + write.addr) := write.data
    }
  }

  // 将所有端口连接到 B 数组
  for (level <- 1 to count_of_levels) {
    val idx = level - 1
    connect_read_port(b_this_read_ports(idx), level)
    connect_write_port(b_this_write_ports(idx), level)
    connect_read_port(b_lc_read_ports(idx), level)
    connect_read_port(b_rc_read_ports(idx), level)
  }
}
