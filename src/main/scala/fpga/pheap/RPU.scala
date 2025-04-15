package fpga.pheap

import chisel3._
import chisel3.util._
import fpga.mem._
import fpga.Const._


// TODO 不要传一整个Memory，传Node就可以
// parent_pos 上->下
// lc_node, rc_node 下->上
// lc_pos, rc_pos可以由parent_pos计算得出，进而读取到node
// cur_node根据token_in，在lc_node, rc_node中选取
class RPU(val level: Int) extends Module {
    val io = IO(new Bundle {
        val token_in = Input(new TokenNode(level))
        val token_out = Output(new TokenNode(level))
        val mem_in = Input(new MemoryIO(level))
        val mem_out = Output(new MemoryIO(level))
    })
    val memory = Module(new MemoryIO(level))
    val token = RegInit(TokenNode.init(level))

    token := token_in
    // io.token_out := token
    io.mem_out := memory
    io.token_out := TokenNode.init(level)

    val addr = token_in.position
    val cur_node = memory.read(addr) // B[i]
    val lc_pos = get_lc_pos(level, io.token_in.entry)
    val rc_pos = lc_pos + 1.U
    val lc_node = mem_in.read(lc_pos)
    val rc_node = mem_in.read(rc_pos)
    
    val cmp_cur_lc = cur_node < lc_node
    val cmp_cur_rc = cur_node < rc_node
    val cmp_lc_rc = lc_node < rc_node

    def local_enqueue() = {
        when (!cur_node.entry.existing) {
            val newNode = Wire(Node.init(level, io.token_in.entry))
            newNode.capacity := newNode.capacity - 1.U
            memory.write(addr, newNode)
            token.op = Operator.nop
        } .elsewhen (cur_node.entry < io.token_in.entry) {
            memory.write(addr, Node.init(level, io.token_in.entry))
            token.entry := cur_node.entry
        } 
        token.position := Mux(lc_node.capacity > 0, lc_pos, rc_pos)
    }

    def local_dequeue() = {
        when (!lc_node.entry.existing && !rc_node.entry.existing) {
            token.op = Operator.nop
        } .elsewhen {
            when(lc_node.entry < rc_node.entry) {
                memory.write(addr, Node.init(level, rc_node.entry, cur_node.capacity))       
                mem_in.write(rc_pos, Node.init(level + 1, Entry.default, rc_node.capacity + 1))
                token.position := rc_pos
            } .elsewhen {
                memory.write(addr, Node.init(level, lc_node.entry, cur_node.capacity))       
                mem_in.write(lc_pos, Node.init(level + 1, Entry.default, lc_node.capacity + 1))
                token.position := lc_pos
            }
        }
    }

    def local_enqueue_dequeue() = {
        when (!lc_node.entry.existing && !rc_node.entry.existing) {
            token.op = Operator.nop
        } .elsewhen {
            when (!cmp_cur_lc && !cmp_cur_rc) {
                token.op = Operator.nop
            } .elsewhen {
                memory.write(addr, Mux(cmp_lc_rc, rc_node, lc_node))
                mem_in.write(Mux(cmp_lc_rc, rc_pos, lc_pos), cur_node)
                token.position := Mux(cmp_lc_rc, rc_pos, lc_pos)
            }
        }
    }

    // 怎么保证操作完成后再将token向下传递? FSM?
    val sCycle0 :: sCycle1 :: sCycle2 :: sIdle :: Nil = Enum(4)
    val state = RegInit(sIdle)

    switch(state) {
        is(sIdle) {
            when(io.token_in.op =/= Operator.nop) {
                token := io.token_in  
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
            local_dequeue
        } .elsewhen {
            local_enqueue_dequeue
        }
    } .otherwise {
        local_enqueue
    }

    def ~>(next: RPU) = {
        next.io.token_in := this.io.token_out
        this.io.mem_in := next.io.mem_out
    }

    def init_memory(): Unit = {
        for(i <- 0 until get_data_depth(level)) {
            val node = Node.init(level, 0)
            memory.write(i, node)
        }
    }
}
