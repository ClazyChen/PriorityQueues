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
        val addr     = Input(UInt(get_addr_width(level).W))
        val en       = Input(Bool())
        val wen      = Input(Bool())
        val pair_in  = Input(new Pair(level))
        val pair_out = Output(new Pair(level))
        val init_done_out = Output(Bool())
    })

    private val depth = get_pair_depth(level)
    private val width = get_pair_width(level)

    // 底层存储
    private val mem = if (use_sram) Module(new SinglePortSram(depth, width))
                      else          Module(new SinglePortFFMem(depth, width))

    // === 1) init_memory() 返回的 done 信号 ===
    private val init_done = init_memory()
    io.init_done_out := init_done

    // === 2) 正常读写，等 init_done_out 后才打开 ===
    mem.idle()
    // 添加寄存器保存最后一次读取的结果
    private val last_read = RegInit(Pair.default(level))
    io.pair_out := last_read // 默认输出上次读取的结果
    
    when (init_done && io.en) {
        when (io.wen) {
            mem.write(io.addr, io.pair_in.asUInt)
        } .otherwise {
            // 读取新数据并更新寄存器和输出
            val read_data = mem.read(io.addr).asTypeOf(new Pair(level))
            io.pair_out := read_data
            last_read := read_data
        }
    }

    /** 
     * 硬件化 init-memory： 
     * 用一个计数器 cnt 从 0 到 depth-1，每拍写入 Pair.default(level)
     * 完成后拉高 done
     */
    private def init_memory(): Bool = {
        val cnt  = RegInit(0.U(log2Ceil(depth).W))
        val done = RegInit(false.B)

        when (!done) {
            // 按照 cnt 从 init_vec 中读取常量，写到 mem 中
            mem.write(cnt, io.pair_in.asUInt)
            // printf(p"Writing level=$level, cnt=$cnt, c=${io.pair_in.asUInt.asTypeOf(new Pair(level)).first.capacity}\n")
            cnt := cnt + 1.U
            when (cnt === (depth-1).U) {
                done := true.B
            }
        }
        done
    }
}