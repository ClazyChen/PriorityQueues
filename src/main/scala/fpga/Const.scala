package fpga

// 定义priorityqueue的规格参数

object Const {

    // number of entries in the priority queue
    val count_of_entries = 64;

    // the width of metadata
    val metadata_width = 32;

    // the width of the rank
    val rank_width = 16;

    // number of levels in the P-heap
    val count_of_levels = 256;

}