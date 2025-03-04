import chisel3._

class Block(dataWidth: Int, pWidth: Int) extends Module {
    val io = IO(new Bundle {
        val read = Input(Bool())
        val write = Input(Bool())
        val new_entry = Input(UInt(dataWidth.W + pWidth.W))
        val left = Input(UInt(dataWidth.W + pWidth.W))
        val right = Input(UInt(dataWidth.W + pWidth.W))
        val output = Output(UInt(dataWidth.W + pWidth.W))
    })

    val entry = RegInit(~0.U(dataWidth.W + pWidth.W))
    io.output := entry

    when(io.read && ~io.write) {
        entry := io.left
    }.elsewhen(~io.read && io.write) {
        when(entry(pWidth - 1, 0) > io.new_entry(pWidth - 1, 0)) {
            when(io.new_entry(pWidth - 1, 0) > io.right(pWidth - 1, 0)) {
                entry := io.new_entry
            }.otherwise {
                entry := io.right
            }
        }
    }
}

class ShiftRegister(dataWidth: Int, pWidth: Int, blocksNum: Int) extends Module {
    val io = IO(new Bundle {
        val read = Input(Bool())
        val write = Input(Bool())
        val new_entry = Input(UInt(dataWidth.W + pWidth.W))
        val output = Output(UInt(dataWidth.W +pWidth.W))
    })

    val blocks = Seq.fill(blocksNum)(Module(new Block(dataWidth, pWidth)))
    io.output := blocks(0).io.output
    blocks(0).io.right := 0.U(dataWidth.W + pWidth.W)
    blocks(blocksNum - 1).io.left := ~0.U(dataWidth.W + pWidth.W)

    for(i <- 0 until blocksNum) {
        blocks(i).io.read := io.read
        blocks(i).io.write := io.write
        blocks(i).io.new_entry := io.new_entry
        if(i != 0) {
            blocks(i).io.right := blocks(i - 1).io.output
        }
        if(i != blocksNum - 1) {
            blocks(i).io.left := blocks(i + 1).io.output
        }
    }
}   


object Main extends App {
    println("Hello Chisel World")
    emitVerilog(new ShiftRegister(4, 4, 4))
}