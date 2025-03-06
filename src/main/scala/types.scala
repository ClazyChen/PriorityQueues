import chisel3._

class Entry(dataWidth: Int, pWidth: Int) extends Bundle {
    val metadata = UInt(dataWidth.W)
    val priority = UInt(pWidth.W)
}

object Entry {
    // 定义默认初始化方法
    def apply(dataWidth: Int, pWidth: Int): Entry = {
        val struct = Wire(new Entry(dataWidth, pWidth))
        struct.metadata := 0.U(dataWidth.W)
        struct.priority := -1.S(pWidth.W).asUInt
        struct
    }

//    def apply(dataWidth: Int, pWidth: Int, data:Int, priority:Int): Entry = {
//        val struct = Wire(new Entry(dataWidth, pWidth))
//        struct.metadata := data.U
//        struct.priority := priority.U
//        struct
//    }
}