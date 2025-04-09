package fpga

// the constants for the priority queue

object Const {

    // number of entries in the priority queue
    val count_of_entries = 64

    // the width of the metadata
    val metadata_width = 32

    // the width of the rank
    val rank_width = 16

    // TODO 这个常量不知道放哪里，因为不是公共的常量
    // 但如果放pheap包里，两个object Const看起来不好
    // 或者level根据count_of_entries计算也行？
    // const for pheap
    val pheap_level = 6
}
