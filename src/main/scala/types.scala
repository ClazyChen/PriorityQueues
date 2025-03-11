import chisel3._

class Entry(dataWidth: Int, rankWidth: Int) extends Bundle {
    val metadata = UInt(dataWidth.W)
    val rank = UInt(rankWidth.W)
}

object Entry {
    def default(dataWidth: Int, rankWidth: Int): Entry = {
        val entry = Wire(new Entry(dataWidth, rankWidth))
        entry.metadata := 0.U(dataWidth.W) // DontCare
        entry.rank := -1.S(rankWidth.W).asUInt
        entry
    }
}