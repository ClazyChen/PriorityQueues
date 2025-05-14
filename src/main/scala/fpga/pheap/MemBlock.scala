package fpga.pheap

import scala._
import chisel3._
import chisel3.util._
import fpga._
import fpga.Const._
import fpga.Node
import fpga.mem
import fpga.pheap.Func._
import fpga.pheap.FFMem._


// 每一层RPU内部的Memory单元,希望选用Sram来实现
class MemBlock (val level : Int,val mem_type : String) extends Module { 
    // data_depth(addr_width)    data_width
    val local_data_depth = UInt(1 << (level - 1)) // 这一层存储node的数量
    val local_data_width = (Node.default(level).asUInt.getWidth).W

    // MemBlock IO (in a RPU)
    val io = IO(new Bundle {
        val node_in = Input(new Node(level))
        val pair_out = Output(new Pair(level)) // read children nodes
        val node_out = Output(new Node(level)) // read node in current level
        val read_node = Input(Bool())
        val read_children = Input(Bool())
        val write = Input(Bool())
        val position_in = Input(UInt(position_width(level).W)) // 传入一个global position
    })

    // different type of memory
    switch (mem_type) {
        is ("Sram") {
            val memory = Module(new Sram(local_data_depth,local_data_width))
        }
        is ("SinglePortSram") {
            val memory = Module(new SinglePortSram(local_data_depth,local_data_width))
        }
        is ("FFMem") {
            val memory = Module(new FFMem(local_data_depth,local_data_width))
        }
        is ("SinglePortFFMem") {
            val memory = Module(new SinglePortFFMem(local_data_depth,local_data_width))
        }
    }
    
    // 端口初始化
    io.node_out := DontCare
    io.pair_out := DontCare

    // 根据io.position_in计算local_index
    val local_index_reg = RegInit(0.U(positionWidth(level).W))

    // 在存储单元中也使用一个状态机  
    val mCycle0 :: mCycle1 :: mCycle2 :: Nil = Enum(3)
    val mem_state_reg = RegInit(mCycle0)
    val left_node = RegInit(Node.default)
    val current_node = RegInit(Node.default)
    val local_index = Wire(UInt((position_width(level)).W))
    val pair_node = Wire(UInt((Pair.default.asUInt).W))
    val node = Wire(UInt((Node.default.asUInt).W))
    val left = Wire(UInt((Node.default.asUInt).W))
    val right = Wire(UInt((Node.default.asUInt).W))

    // 操作sram单元
    // sram每次读出来一个pair类型，包含两个node，这个过程不能在一个周期内完成
    // 上层read/write信号的保持由上层模块负责实现
    switch (mem_state_reg) {
        is (mCycle0) {
            when (io.read_node) {
                local_index = get_local_index(io.position_in, level)
                local_index_reg := local_index
                node = memory.read(local_index).asTypeOf(new Node(level)) // 读出的变量用node保存
                mem_state_reg := mCycle1
            }.elsewhen (io.read_children) {
                local_index = get_local_index(io.position_in, level)
                local_index_reg := local_index
                left = memory.read(local_index).asTypeOf(new Node(level))
                mem_state_reg := mCycle1
            }.elsewhen (io.write) {
                local_index = get_local_index(io.position_in, level)
                mem.write(local_index, io.node_in.asUInt)
                mem_state_reg := mCycle0
            }.otherwise {}
        }
        is (mCycle1) { // read children nodes : need more cycles
            when (io.read_node && !io.write) {
                current_node := node
                mem_state_reg := mCycle2 // current_node valid
            }.elsewhen (io.read_node && io.write) {
                local_index = get_local_index(io.position_in, level)
                io.node_out := node
                current_node.position := token.position
                current_node.value := token.op.push
                memory.write(local_index, io.node_in.asUInt) // 提前修改position为1的结点
                mem_state_reg := mCycle2
            }.elsewhen (io.read_children) {
                left_node := left
                right = memory.read(local_index_reg + 1.U).asTypeOf(new Node(level))
                mem_state_reg := mCycle2
            }.otherwise {}
        }
        is (mCycle2) { // output
            io.node_out := current_node
            io.pair_out.left_node := left_node
            io.pair_out.right_node := right
            when (io.write) {
                local_index = get_local_index(io.position_in, level)
                memory.write(local_index, io.node_in.asUInt)
            }
            mem_state_reg := mCycle0
        }
    }

}
