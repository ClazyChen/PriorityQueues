import chisel3._

class Entry(dataWidth: Int, rWidth: Int) extends Bundle {
    val metadata = UInt(dataWidth.W)
    val rank = UInt(rWidth.W)
}

object Entry {
    def default(dataWidth: Int, rWidth: Int): Entry = {
        val entry = Wire(new Entry(dataWidth, rWidth))
        entry.metadata := 0.U(dataWidth.W)
        entry.rank := -1.S(rWidth.W).asUInt
        entry
    }
}