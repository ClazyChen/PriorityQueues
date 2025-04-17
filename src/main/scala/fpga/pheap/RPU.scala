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
        val cur_pos_out = Input(UInt(position_width(level).W))
        val lc_node_in = Input(new Node(level + 1))
        val rc_node_in = Input(new Node(level + 1))
        val left_node_out = Output(new Node(level))
        val right_node_out = Output(new Node(level))
    })
    val memory = Module(new Memory(level))
    val token = RegInit(TokenNode.init(level))

    token := io.token_in
    io.token_out := TokenNode.init(level)

    val addr = io.token_in.position
    val left_pos = get_lc_pos(level - 1, io.parent_pos_in)
    val right_pos = left_pos + 1.U
    val lc_pos = get_lc_pos(level, io.token_in.position)
    val rc_pos = lc_pos + 1.U
    val left_node = memory.read(left_pos)
    val right_node = memory.read(right_pos)
    val cur_node = Mux(left_pos === io.token_in.position, left_node, right_node)

    io.cur_pos_out := addr
    io.left_node_out := left_node
    io.right_node_out := right_node
    
    val cmp_token_lc = io.token_in.entry < io.lc_node_in.entry
    val cmp_token_rc = io.token_in.entry < io.rc_node_in.entry
    val cmp_lc_rc = io.lc_node_in.entry < io.rc_node_in.entry

    def local_enqueue() = {
        val cur_capacity = cur_node.capacity - 1.U
        when (!cur_node.entry.existing) {
            memory.write(addr, Node.init(level, io.token_in.entry, cur_capacity))
            token.op := Operator.nop
        } .elsewhen (cur_node.entry < io.token_in.entry) {
            memory.write(addr, Node.init(level, io.token_in.entry, cur_capacity))
            token.entry := cur_node.entry
        } .otherwise {
            // 论文里没提到,但是应该要cur_node.capacity - 1.U吧?
            memory.write(addr, Node.init(level, cur_node.entry, cur_capacity))
        }
        token.position := Mux(io.lc_node_in.capacity > 0.U, lc_pos, rc_pos)
    }

    def local_dequeue() = {
        // 论文里说Increment B[k].capacity, 我感觉不合理
        // 这里实现的是仅增加当前层Node的capacity
        val cur_capacity = cur_node.capacity + 1.U
        when (!io.lc_node_in.entry.existing && !io.rc_node_in.entry.existing) {
            memory.write(addr, Node.init(level, Entry.default, cur_capacity))
            token.op := Operator.nop
        } .otherwise {
            when(io.lc_node_in.entry < io.rc_node_in.entry) {
                memory.write(addr, Node.init(level, io.rc_node_in.entry, cur_capacity))       
                token.position := rc_pos
            } .otherwise {
                memory.write(addr, Node.init(level, io.lc_node_in.entry, cur_capacity))       
                token.position := lc_pos
            }
        }
    }

    def local_enqueue_dequeue() = {
        when (!io.lc_node_in.entry.existing && !io.rc_node_in.entry.existing) {
            // 这里的实现与原文不同
            // 三角形中包括token, lc_node, rc_node
            // 因为在根节点cur_node与V比较完之后,并不能在外部马上将结果写入
            // 所以只能先用token将比较结果传进RPU
            memory.write(addr, Node.init(level, io.token_in.entry, get_capacity(level) - 1.U) )
            token.op := Operator.nop
        } .otherwise {
            when (!cmp_token_lc && !cmp_token_rc) {
                memory.write(addr, Node.init(level, io.token_in.entry, get_capacity(level) - 1.U) )
                token.op := Operator.nop
            } .otherwise {
                memory.write(addr, Mux(cmp_lc_rc, io.rc_node_in, io.lc_node_in))
                token.entry := cur_node
                token.position := Mux(cmp_lc_rc, rc_pos, lc_pos)
            }
        }
    }

    // 保证操作完成后再将token向下传递
    val sCycle0 :: sCycle1 :: sCycle2 :: sIdle :: Nil = Enum(4)
    val state = RegInit(sIdle)

    switch(state) {
        is(sIdle) {
            when(io.token_in.op.pop || io.token_in.op.push.existing) {
                state := sCycle0
            }
            io.token_out := TokenNode.init(level) 
        }
        is(sCycle0) {
            io.token_out := TokenNode.init(level) 
            state := sCycle1
        }
        is(sCycle1) {
            io.token_out := TokenNode.init(level) 
            state := sCycle2
        }
        is(sCycle2) {
            io.token_out := token            
            state := sIdle                   
        }
    }


    when (io.token_in.op.pop) {
        when(io.token_in.op.push.existing) {
            local_dequeue()
        } .otherwise {
            local_enqueue_dequeue()
        }
    } .otherwise {
        local_enqueue()
    }

    def ~>(next: RPU) = {
        next.io.token_in := this.io.token_out
        next.io.parent_pos_in := this.io.cur_pos_out
        this.io.lc_node_in := next.io.left_node_out
        this.io.rc_node_in := next.io.right_node_out
    }

    def init_memory(): Unit = {
        for(i <- 0 until get_data_depth(level)) {
            val node = Node.init(level, Entry.default, get_capacity(level))
            memory.write(i.U, node)
        }
    }
}
