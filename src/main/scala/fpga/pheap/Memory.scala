package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga._
import fpga.Const._
import fpga.pheap.Param._


class Memory(
    val level: Int,
    val use_sram: Boolean = use_sram_param     
) extends Module {
  val io = IO(new Bundle {
    val addr           = Input(UInt(get_addr_width(level).W))
    val en             = Input(Bool())
    val wen            = Input(Bool())
    val pair_in        = Input(new Pair(level))
    val pair_out       = Output(new Pair(level))
    val init_done_out  = Output(Bool())
  })

  private val depth = get_pair_depth(level)
  private val width = get_pair_width(level)
  private val mem   = if (use_sram) Module(new SinglePortSram(depth, width))
                      else          Module(new SinglePortFFMem(depth, width))

  // === 双阶段 init_state: 0 = 写入，1 = 校验，2 = 完成 ===
  private val sWrite :: sCheck :: sDone :: Nil = Enum(3)
  private val init_state = RegInit(sWrite)

  // 两个计数器：写阶段用 writeCnt，校验阶段用 checkCnt
  private val writeCnt = RegInit(0.U(log2Ceil(depth).W))
  private val checkCnt = RegInit(0.U(log2Ceil(depth).W))

  // init_state 的状态机
  switch(init_state) {
    is(sWrite) {
      // 写入 Pair.default(level) 到每一个地址
      mem.write(writeCnt, Pair.default(level).asUInt)
      when(writeCnt === (depth - 1).U) {
        init_state := sCheck
        checkCnt := 0.U
      }.otherwise {
        writeCnt := writeCnt + 1.U
      }
    }

    // TODO check不通过，检查原因
    is(sCheck) {
      // 读回 checkCnt 位置的值，和 Pair.default(level) 比较
      val rd = mem.read(checkCnt).asTypeOf(new Pair(level))
      when(rd.first.capacity === Pair.default(level).first.capacity &&
           rd.second.capacity=== Pair.default(level).second.capacity) {
        // 如果还没到最后地址，继续校验下一个
        when(checkCnt === (depth - 1).U) {
          init_state := sDone
        }.otherwise {
          checkCnt := checkCnt + 1.U
        }
      }.otherwise {
        // 只要有一个不匹配，就回到写阶段重写一遍
        init_state := sWrite
        writeCnt := 0.U
      }
    }

    is(sDone) {
      // 完成后什么也不做
    }
  }

  // expose init_done_out 只有状态机进到 sDone 才为 true
  io.init_done_out := (init_state === sDone)

  // 正常读写，等 init_done 后才打开
  mem.idle()
  private val last_read = RegInit(Pair.default(level))
  io.pair_out := last_read

  when(io.init_done_out && io.en) {
    when(io.wen) {
      mem.write(io.addr, io.pair_in.asUInt)
    }.otherwise {
      val read_data = mem.read(io.addr).asTypeOf(new Pair(level))
      last_read := read_data
      io.pair_out := read_data
    }
  }
}
