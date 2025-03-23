import chisel3._

class Entry(val dataWidth: Int, val rankWidth: Int) extends Bundle {
    val data = UInt(dataWidth.W)
    val rank = UInt(rankWidth.W)
}

object Entry {
    def default(dataWidth: Int, rankWidth: Int): Entry = {
        val e = Wire(new Entry(dataWidth, rankWidth))
        // 默认rank为最低优先级
        e.rank := -1.S(rankWidth.W).asUInt
        e.data := 0.U(dataWidth.W)
        e
    }
}

// 每个block存储一个entry,通过next和prev与相邻block通信
class Block(val dataWidth: Int, val rankWidth: Int) extends Module {
    val io = IO(new Bundle {
        val read_in = Input(Bool())
        val write_in = Input(Bool())
        val cmp_prev_in = Input(Bool()) // new_entry_in < prev 为1
        val cmp_next_in = Input(Bool())
        val new_entry_in = Input(new Entry(dataWidth, rankWidth))
        val next_in = Input(new Entry(dataWidth, rankWidth))
        val prev_in = Input(new Entry(dataWidth, rankWidth))
        val output_out = Output(new Entry(dataWidth, rankWidth))
        val cmp_out = Output(Bool())
    })

    val entry = RegInit(Entry.default(dataWidth, rankWidth))
    io.output_out := entry

    io.cmp_out := Mux(entry.rank > io.new_entry_in.rank, true.B, false.B)

    // 高优先级的block(rank较小的)放在右边
    when(io.read_in && !io.write_in) {
        // dequeue操作,entry依次右移
        entry := io.next_in
        }.elsewhen(!io.read_in && io.write_in) { // enqueue操作
            when(io.cmp_out) { // 当前优先级小于新entry优先级
                entry := Mux(io.cmp_prev_in, io.prev_in, io.new_entry_in)
            } // 当前优先级高于新entry优先级,不移动
        }.elsewhen(io.read_in && io.write_in) {
            when(!io.cmp_out) {
                entry := Mux(io.cmp_next_in, io.new_entry_in, io.next_in)
            }
        }
}

class ShiftRegister(val dataWidth: Int, val rankWidth: Int, val blocksNum: Int, debug: Boolean = false)
    extends Module {
    val io = IO(new Bundle {
        val read_in = Input(Bool())
        val write_in = Input(Bool())
        val new_entry_in = Input(new Entry(dataWidth, rankWidth))
        val output_out = Output(new Entry(dataWidth, rankWidth))
        // 添加debug可选端口
        val dbgPort = if(debug) Some(Output(Vec(blocksNum, new Entry(dataWidth, rankWidth)))) else None
    })

    val blocks = Seq.fill(blocksNum)(Module(new Block(dataWidth, rankWidth)))
    val highest = RegNext(blocks(0).io.output_out)
    
    blocks(0).io.cmp_prev_in := false.B
    blocks(0).io.prev_in := Entry.default(dataWidth, rankWidth)
    blocks(0).io.prev_in.rank := 0.U
    
    blocks(blocksNum - 1).io.next_in := Entry.default(dataWidth, rankWidth)
    blocks(blocksNum - 1).io.cmp_next_in := true.B

    for (i <- 0 until blocksNum) {
        blocks(i).io.read_in := io.read_in
        blocks(i).io.write_in := io.write_in
        blocks(i).io.new_entry_in := io.new_entry_in
        if (i != 0) {
            blocks(i).io.prev_in := blocks(i - 1).io.output_out
            blocks(i).io.cmp_prev_in := blocks(i - 1).io.cmp_out
        }
        if (i != blocksNum - 1) {
            blocks(i).io.next_in := blocks(i + 1).io.output_out
            blocks(i).io.cmp_next_in := blocks(i + 1).io.cmp_out
        }
        
        if(debug) {
            io.dbgPort.get(i) := blocks(i).io.output_out
        }
    }

    when(io.read_in && io.write_in) {
        when(io.new_entry_in.rank < highest.rank) {
            io.output_out := io.new_entry_in
        }.otherwise {
            io.output_out := highest
        }
    }.otherwise {
        io.output_out := blocks(0).io.output_out
    }

}

