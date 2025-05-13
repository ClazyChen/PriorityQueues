package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga._
import fpga.Const._
import fpga.pheap.Param._

// parent_pos 上->下
// lc_node, rc_node 下->上
// lc_pos, rc_pos可以由parent_pos计算得出，进而读取到node
// cur_node根据token_in，在lc_node, rc_node中选取
class RPU(val level: Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new TokenNode(level))
        val token_out = Output(new TokenNode(level + 1))
        val parent_pos_in = Input(UInt(position_width(level - 1).W))
        val cur_pos_out = Output(UInt(position_width(level).W))
        val lc_node_in = Input(new Node(level + 1))
        val rc_node_in = Input(new Node(level + 1))
        val left_node_out = Output(new Node(level))
        val right_node_out = Output(new Node(level))
    })
    // mem初始化需要get_pair_depth个周期，写入空pair
    val mem = Module(new Memory(level))

    // 应该对token_in和token_out进行区分
    // token_in存储输入的io.token_in，因为io.token_in信号不稳定
    // token_out对应于对下一级rpu的操作，需要根据token_in的信号来确定
    val token_in = RegInit(io.token_in)
    val token_out = RegInit(TokenNode.default(level))
    
    // token_out只在cycle1状态进行输出，其他状态不输出
    // 也就是说，除了level1,其他level的token_in信号都只能在idle状态下接收到
    // 在下一个状态，token_in信号将变成无效值
    io.token_out := TokenNode.default(level)

    val addr = pos2addr(level, token_in.position)
    val lc_pos = RegInit(get_lc_pos(io.token_in.position))
    val rc_pos = RegInit(get_rc_pos(io.token_in.position))
    val left_node = Wire(new Node(level))
    val right_node = Wire(new Node(level))
    val left_pos = get_lc_pos(io.parent_pos_in)
    val cur_node = Mux(left_pos === token_in.position, left_node, right_node)
    val new_pair = Wire(new Pair(level))
    val new_node = Wire(new Node(level))

    new_pair := Pair.default(level)
    new_node := Node.default(level)
    idle()

    left_node := mem.io.pair_out.first
    right_node := mem.io.pair_out.second
    io.cur_pos_out := token_in.position
    io.left_node_out := left_node
    io.right_node_out := right_node
    
    // 用2个比较器加快local_enqueue_dequeue
    val cmp_token_lc = io.lc_node_in.entry > token_in.op.push
    val cmp_token_rc = io.rc_node_in.entry > token_in.op.push
    val cmp_lc_rc = io.rc_node_in.entry > io.lc_node_in.entry

    def local_enqueue() = {
        val cur_capacity = cur_node.capacity - 1.U
        when (!cur_node.entry.existing) {
            new_node := Node.init(level, token_in.op.push, cur_capacity)
            token_out.op := Operator.nop
        } .elsewhen (cur_node.entry > token_in.op.push) {
            new_node := Node.init(level, cur_node.entry, cur_capacity)
            token_out.op.push := token_in.op.push
        } .otherwise {
            new_node := Node.init(level, token_in.op.push, cur_capacity)
            token_out.op.push := cur_node.entry
        }
 
        token_out.position := Mux(is_empty_node(io.lc_node_in), rc_pos, lc_pos)
    }

    def local_dequeue() = {
        // 论文里说Increment B[k].capacity, 我感觉不合理
        // 这里实现的是仅增加当前层Node的capacity
        val cur_capacity = cur_node.capacity + 1.U
        when (!io.lc_node_in.entry.existing && !io.rc_node_in.entry.existing) {
            new_node := Node.init(level, Entry.default, cur_capacity)
            token_out.op := Operator.nop
        } .otherwise {
            when(io.rc_node_in.entry > io.lc_node_in.entry) {
                new_node := Node.init(level, io.rc_node_in.entry, cur_capacity)     
                token_out.position := rc_pos
            } .otherwise {
                new_node := Node.init(level, io.lc_node_in.entry, cur_capacity)    
                token_out.position := lc_pos
            }
        }
    }

    def local_enqueue_dequeue() = {
        when (!io.lc_node_in.entry.existing && !io.rc_node_in.entry.existing) {
            // 这里的实现与原文不同
            // 三角形中包括token, lc_node, rc_node
            // 因为在根节点cur_node与V比较完之后,并不能在外部马上将结果写入
            // 所以只能先用token将比较结果传进RPU
            new_node := Node.init(level, token_in.op.push, get_capacity(level) - 1.U)
            token_out.op := Operator.nop
        } .otherwise {
            when (!cmp_token_lc && !cmp_token_rc) {
                new_node := Node.init(level, token_in.op.push, get_capacity(level) - 1.U)
                token_out.op := Operator.nop
            } .otherwise {
                new_node := Mux(cmp_lc_rc, io.rc_node_in, io.lc_node_in)
                token_out.op.push := cur_node.entry
                token_out.position := Mux(cmp_lc_rc, rc_pos, lc_pos)
            }
        }
    }

    // 保证操作完成后再将token向下传递
    val sCycle0 :: sCycle1 :: sIdle :: Nil = Enum(3)
    val state = RegInit(sIdle)

    // idle -> cycle0 接收到token，根据token读
    // cycle0 -> cycle1 读到结果，根据结果写，并更新token
    // cycle1 -> idle token传到下一级，mem.idle
    // 如果在cycle1阶段将token传到下一级，那么在下一个周期
    // 当前rpu处于idle状态，下一级rpu处于cycle0状态
    val pass_down = RegInit(false.B)
    switch(state) {
        is(sIdle) {
            when(io.token_in.op.pop || io.token_in.op.push.existing) {
                token_in := io.token_in
                token_out := io.token_in
                lc_pos := get_lc_pos(io.token_in.position)
                rc_pos := get_rc_pos(io.token_in.position)
                // left_node, right_node接到mem的输出上, read后的下一个周期获取到输出
                read(addr)
                state := sCycle0
            }
            when (pass_down) {
                io.token_out := token_out
                pass_down := false.B
            } .otherwise {
                io.token_out := TokenNode.default(level) 
            }
        }
        is(sCycle0) {
            when (token_in.op.pop) {
                when(token_in.op.push.existing) {
                    local_enqueue_dequeue()
                } .otherwise {
                    local_dequeue()
                }
            } .otherwise {
                local_enqueue()
            }
            when(is_left(token_in.position)) {
                new_pair.first := new_node
                new_pair.second := right_node
            }.otherwise {
                new_pair.first := left_node
                new_pair.second := new_node
            }
            write(addr, new_pair)
            state := sCycle1
        }
        is(sCycle1) {
            pass_down := true.B
            idle()
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
