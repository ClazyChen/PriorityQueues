package fpga

// the constants for the priority queue

object Const {

    // number of entries in the priority queue
    val count_of_entries = 64

    // the width of the metadata
    val metadata_width = 32

    // the width of the rank
    val rank_width = 16

    // the type of operation
    val op_nop = 0
    val op_enqueue = 1
    val op_dequeue = 2
    val op_replace = 3
}
