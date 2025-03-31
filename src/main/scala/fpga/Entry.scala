package fpga

import chisel3._
import chisel3.util._
import fpga.Const._

// an entry in the priority queue
// rank = 0 is the highest priority
// rank = 2^rank_width - 1 is the lowest priority, i.e. -1.S(rank_width.W).asUInt
class Entry extends Bundle {
    val existing = Bool() // whether the entry is valid
    val metadata = UInt(metadata_width.W) // metadata
    val rank = UInt(rank_width.W) // rank (priority)
    
    // compare two entries by rank
    def <(that: Entry): Bool = (this.rank < that.rank) || !that.existing
}

// the default entry (invalid entry)
object Entry {
    def default: Entry = {
        val entry = Wire(new Entry)
        entry.existing := false.B
        entry.metadata := 0.U(metadata_width.W)
        entry.rank := -1.S(rank_width.W).asUInt // -1 is the lowest priority
        entry
    }
}

// an operator for the priority queue
class Operator extends Bundle {
	val push = new Entry // push an entry into the priority queue
	val pop = Bool() // pop an entry from the priority queue
} 

object Operator {
    def default = {
        val op = Wire(new Operator)
        op.push := Entry.default
        op.pop := false.B
        op
    }
}

// the io of the priority queue
trait PriorityQueueTrait extends Module {
    class PQIO extends Bundle {
        val op_in = Input(new Operator)
        val entry_out = Output(new Entry)
        val dbg_port = if (debug) Some(Output(Vec(count_of_entries, new Entry))) else None
        // val dbg_port1 = if (debug) Some(Output(Vec(count_of_entries, new Entry))) else None
    }

    val io: PQIO
}