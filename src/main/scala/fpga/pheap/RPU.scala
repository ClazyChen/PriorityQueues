package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga._
import fpga.Const._
import fpga.pheap.Param._


class RPU(val level: Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new TokenNode(level))
        val token_out = Output(new TokenNode(level + 1))
        val pair_in = Input(new Pair(level + 1))
        val pair_out = Output(new Pair(level))
    })
    // mem初始化需要get_pair_depth个周期，写入空pair
    val mem = Module(new Memory(level))

    // 应该对token_in和token_out进行区分
    // token_in存储输入的io.token_in，因为io.token_in信号不稳定，只在第一个周期输入
    // token_out对应于对下一级rpu的操作，需要根据token_in的信号来确定
    val token_in = RegInit(io.token_in)
    val token_out = RegInit(TokenNode.default(level + 1))

    // ffmem在当个周期就能读出数据，下一个周期数据就消失了
    // sram读取本身就会延迟一个周期才能获取到数据
    val pair_in = if(use_sram_param) WireInit(io.pair_in) else RegInit(io.pair_in)
    
    // token_out只在cycle1状态进行输出，其他状态不输出
    // 也就是说，除了level1,其他level的token_in信号都只能在idle状态下接收到
    // 在下一个状态，token_in信号将变成无效值
    io.token_out := TokenNode.default(level + 1)

    val addr = pos2addr(level, token_in.position)
    val lc_pos = RegInit(get_lc_pos(io.token_in.position))
    val rc_pos = RegInit(get_rc_pos(io.token_in.position))

    // 当前level读出的Pair
    val cur_pair = Wire(new Pair(level))
    cur_pair := mem.io.pair_out
    io.pair_out := cur_pair

    // 根据token_in，判断当前节点位置
    val cur_node = Mux(is_left(token_in.position), cur_pair.first, cur_pair.second)

    // 临时存储当前level的节点
    val new_pair = RegInit(Pair.default(level))
    val new_node = WireInit(Node.default(level))

    // 初始化
    // new_pair := Pair.default(level)
    // new_node := Node.default(level)
    idle()
    
    
    // 用3个比较器加快local_enqueue_dequeue
    val cmp_token_lc = pair_in.first.entry > token_in.op.push
    val cmp_token_rc = pair_in.second.entry > token_in.op.push
    val cmp_lc_rc = pair_in.second.entry > pair_in.first.entry
    val cmp_token_cur = cur_node.entry > token_in.op.push

    def local_enqueue() = {
        val cur_capacity = Mux(is_full_node(cur_node), cur_node.capacity, cur_node.capacity - 1.U)
        when (!cur_node.entry.existing) { // 如果当前节点不存在，则直接插入
            new_node := Node.init(level, token_in.op.push, cur_capacity)
            token_out.op := Operator.nop
        } .elsewhen (cmp_token_cur) { // 当前节点更大，token_in传到下一层
            new_node := Node.init(level, cur_node.entry, cur_capacity)
            token_out.op.push := token_in.op.push
            token_out.op.pop := token_in.op.pop
        } .otherwise { // 当前节点更小，当前节点传到下一层
            new_node := Node.init(level, token_in.op.push, cur_capacity)
            token_out.op.push := cur_node.entry
            token_out.op.pop := token_in.op.pop
        }
        // 左孩子未满，则传到左孩子
        val lc_full = is_full_node(pair_in.first)
        token_out.position := Mux(lc_full, rc_pos, lc_pos)
    }

    def local_dequeue() = {
        val cur_capacity = cur_node.capacity + 1.U
        when (!pair_in.first.entry.existing && !pair_in.second.entry.existing) {
            // 如果两个孩子都不存在，当前节点变成空节点
            new_node := Node.init(level, Entry.default, cur_capacity)
            token_out.op := Operator.nop
        } .otherwise {
            // 否则，将更大的孩子写入当前节点
            token_out.op := token_in.op
            when(cmp_lc_rc) {
                new_node := Node.init(level, pair_in.second.entry, cur_capacity)  
                token_out.position := rc_pos
            } .otherwise {
                new_node := Node.init(level, pair_in.first.entry, cur_capacity)    
                token_out.position := lc_pos
            }
        }
    }

    def local_enqueue_dequeue() = {
        when (!pair_in.first.entry.existing && !pair_in.second.entry.existing) {
            new_node := Node.init(level, token_in.op.push, cur_node.capacity)
            token_out.op := Operator.nop
        } .otherwise {
            when (!cmp_token_lc && !cmp_token_rc) {
                new_node := Node.init(level, token_in.op.push, cur_node.capacity)
                token_out.op := Operator.nop
            } .otherwise {
                new_node := Mux(cmp_lc_rc, pair_in.second, pair_in.first)
                new_node.capacity := cur_node.capacity
                token_out.op := token_in.op
                token_out.position := Mux(cmp_lc_rc, rc_pos, lc_pos)
            }
        }
    }

    // 保证操作完成后再将token向下传递
    val sCycle0 :: sCycle1 :: sIdle :: Nil = Enum(3)
    val state = RegInit(sIdle)

    // idle -> cycle0 接收到token，根据token读
    // cycle0 -> cycle1 读到结果，根据结果写，并更新token
    // cycle1 -> idle 准备好token，并进入idle状态
    // 如果在cycle1阶段将token传到下一级，那么在下一个周期
    // 当前rpu处于idle状态，下一级rpu处于cycle0状态
    val pass_down = RegInit(false.B)
    switch(state) {
        is(sIdle) {
            // cur_pair接到mem的输出上, read后的下一个周期获取到输出
            // read(addr) token_in在下一个周期才完成更新，读取到mem的输出还需要一个周期
            read(pos2addr(level, io.token_in.position))
            val start = io.token_in.op.pop || io.token_in.op.push.existing
            when(io.token_in.active) {
                token_in.position := io.token_in.position
            }
            when(start) {
                token_in := io.token_in
                pair_in := io.pair_in
                lc_pos := get_lc_pos(io.token_in.position)
                rc_pos := get_rc_pos(io.token_in.position)
                state := sCycle0
            }
            when (pass_down) { // 刚完成一个操作
                io.token_out := token_out
                pass_down := false.B
            } .elsewhen(start) {
                // 即将开始一个操作,读取子节点
                val nop_token = TokenNode.default(level + 1)
                nop_token.active := true.B
                nop_token.position := get_lc_pos(io.token_in.position)
                io.token_out := nop_token
            }. otherwise {  // 空闲
                io.token_out := TokenNode.default(level + 1)
            }
        }
        is(sCycle0) {
            read(addr)
            when (token_in.op.pop) {
                when(token_in.op.push.existing) {
                    local_enqueue_dequeue()
                } .otherwise {
                    local_dequeue()
                }
            } .otherwise {
                local_enqueue()
            }

            // 对当前level的节点进行更新
            when(is_left(token_in.position)) {
                new_pair.first := new_node
                new_pair.second := cur_pair.second
            }.otherwise {
                new_pair.first := cur_pair.first
                new_pair.second := new_node
            }

            
            state := sCycle1
        }
        is(sCycle1) {
            // write只在这个阶段进行
            write(addr, new_pair)
            pass_down := true.B
            // idle()
            state := sIdle
        }
    }

    def read(addr: UInt) = {
        mem.io.en := true.B
        mem.io.wen := false.B
        mem.io.addr := addr
        mem.io.pair_in := Pair.default(level)
        mem.io.pair_out
    }

    def write(addr: UInt, pair: Pair) = {
        mem.io.en := true.B
        mem.io.wen := true.B
        mem.io.addr := addr
        mem.io.pair_in := pair
    }

    def idle(): Unit = {
        mem.io.en := false.B
        mem.io.wen := DontCare
        mem.io.addr := DontCare
        mem.io.pair_in := Pair.default(level)
    }

}
