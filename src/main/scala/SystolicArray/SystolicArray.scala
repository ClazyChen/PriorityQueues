import chisel3._
import chisel3.util._


class SystolicBlock(dataWidth: Int, rankWidth: Int) extends Module {
  val io = IO(new Bundle {
    val enq_in  = Input(Bool())
    val deq_in  = Input(Bool())
    val enq_out = Output(Bool())
    val deq_out = Output(Bool())

    val bubble_in  = Input(new Entry(dataWidth, rankWidth))
    val left = Input(new Entry(dataWidth, rankWidth))
    val bubble_out = Output(new Entry(dataWidth, rankWidth))
    val output = Output(new Entry(dataWidth, rankWidth))
  })

  val stored = RegInit(Entry.default(dataWidth, rankWidth))
  val bubble = RegInit(Entry.default(dataWidth, rankWidth))

  io.output := stored
  io.bubble_out := bubble

  when(io.enq_in && !io.deq_in) {
    when(stored.rank > io.bubble_in.rank) {
      bubble := stored
      stored := io.bubble_in
    } .otherwise {
      bubble := io.bubble_in
    }
  } .elsewhen(io.deq_in && !io.enq_in) {
    stored := io.left
  } .otherwise {
    bubble := Entry.default(dataWidth, rankWidth)
  }

  io.enq_out := RegNext(io.enq_in, init = false.B)
  io.deq_out := RegNext(io.deq_in, init = false.B)
}


class SystolicArray(dataWidth: Int, rankWidth: Int, blocksNum: Int) extends Module {
  val io = IO(new Bundle {
    val read  = Input(Bool())   
    val write = Input(Bool())  
    val new_entry = Input(new Entry(dataWidth, rankWidth))
    val output = Output(new Entry(dataWidth, rankWidth))
  })

  val blocks = Seq.fill(blocksNum)(Module(new SystolicBlock(dataWidth, rankWidth)))

  blocks(0).io.enq_in := io.write
  blocks(0).io.deq_in := io.read
  blocks(0).io.bubble_in := io.new_entry
  if (blocksNum > 1) {
    blocks(0).io.left := blocks(1).io.output
  } 
  else {
    blocks(0).io.left := Entry.default(dataWidth, rankWidth)
  } 

  for(i <- 1 until blocksNum) {
    blocks(i).io.enq_in := blocks(i - 1).io.enq_out
    blocks(i).io.deq_in := blocks(i - 1).io.deq_out
    blocks(i).io.bubble_in := blocks(i - 1).io.bubble_out
    if (i < blocksNum - 1) {
      blocks(i).io.left := blocks(i + 1).io.output
    }
    else {
      blocks(i).io.left := Entry.default(dataWidth, rankWidth)
    }
  }

  io.output := blocks(0).io.output
}
