package fpga.pheap

object Const {

  // number of levels in the pheap
  val count_of_levels = 4

  // the width of the rank
  val data_width = 16

  // number of entries in the pheap
  val total_nodes = (1 << count_of_levels) - 1
}
