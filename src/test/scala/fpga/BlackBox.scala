package fpga

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.PriorityQueue
import fpga.Const._
import fpga.pheap.Param._

// a black box for the priority queue
object BlackBox {

    // cold start operations (all of them are push operations)
    // val cold_start_ops = count_of_entries
    val cold_start_ops = 3

    // number of operations in the test
    val num_ops = 100

    // push, pop, replace ratio
    val ratio = (0.4, 0.3, 0.4)
    val op_nop = -1
    val op_push = 0
    val op_pop = 1
    val op_replace = 2

    def to_op(r: Double): Int = {
        if (r < ratio._1) op_push
        else if (r < ratio._1 + ratio._2) op_pop
        else op_replace
    }

    // random seed
    val seed = 1234567891

    def nop[PQ <: PriorityQueueTrait](implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        pq.io.op_in.push.existing.poke(false.B)
        pq.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
        pq.io.op_in.push.metadata.poke(0.U)
        pq.io.op_in.pop.poke(false.B)
        pq.clock.step()
    }

    // push a new entry into the priority queue
    def push[PQ <: PriorityQueueTrait](rank: Int, metadata: Int)(implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        pq.io.op_in.push.existing.poke(true.B)
        pq.io.op_in.push.rank.poke(rank.U)
        pq.io.op_in.push.metadata.poke(metadata.U)
        pq.io.op_in.pop.poke(false.B)
        pq.clock.step()
        std_pq.enqueue((rank, metadata))
    }

    // pop the top entry from the priority queue
    def pop[PQ <: PriorityQueueTrait](implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        pq.io.op_in.push.existing.poke(false.B)
        pq.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
        pq.io.op_in.push.metadata.poke(0.U)
        pq.io.op_in.pop.poke(true.B)
        pq.clock.step()
        std_pq.dequeue()
    }

    // replace the top entry with a new entry
    def replace[PQ <: PriorityQueueTrait](rank: Int, metadata: Int)(implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        pq.io.op_in.push.existing.poke(true.B)
        pq.io.op_in.push.rank.poke(rank.U)
        pq.io.op_in.push.metadata.poke(metadata.U)
        pq.io.op_in.pop.poke(true.B)
        pq.clock.step()
        std_pq.enqueue((rank, metadata))
        std_pq.dequeue()
    }

    // check the top entry of the priority queue
    def check_top[PQ <: PriorityQueueTrait](implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        if (std_pq.isEmpty) {
            pq.io.entry_out.existing.expect(false.B)
        } else {
            pq.io.entry_out.existing.expect(true.B)
            pq.io.entry_out.rank.expect(std_pq.head._1.U)
            // pq.io.entry_out.metadata.expect(std_pq.head._2.U)
        }
    }

    val random = new Random(seed)

    def random_ops(): Array[Int] = {
        Array.fill(num_ops)(to_op(random.nextDouble()))
    }

    // generate a sequence of push and pop operations
    // push, pop, push, pop, ...
    def push_pop_ops(): Array[Int] = {
        (0 until num_ops).map(i => if (i % 2 == 0) op_push else op_pop).toArray
    }

    def pheap_ops(): Array[Int] = {
        val base_ops = Array.fill(num_ops)(to_op(random.nextDouble()))
        base_ops.flatMap(op => Array(op) ++ Array.fill(5)(op_nop))
    }



    // the test body
    def test_black_box[PQ <: PriorityQueueTrait](c: PQ, test_ops: Array[Int]): Unit = {
        // generate the cold start numbers and the test numbers
        val cold_start_nums = Array.fill(cold_start_ops)(random.nextInt(1 << rank_width))
        val test_data_ops = test_ops.count(op => op == op_push || op == op_replace)
        val test_nums = Array.fill(test_data_ops)(random.nextInt(1 << rank_width))

        // the built-in priority queue to check the result, and the priority queue to test
        // lazy implicit val std_pq = new PriorityQueue[(Int, Int)]()(Ordering.by((x: (Int, Int)) => (x._1, x._2)).reverse)
        // PHeap的优先级与前面的几个结构相反
        lazy implicit val std_pq = new PriorityQueue[(Int, Int)]()(Ordering.by((x: (Int, Int)) => (x._1, x._2)))
        lazy implicit val pq = c

        pq.clock.step(get_pair_depth(count_of_levels))

        // initialize the priority queue
        cold_start_nums.zipWithIndex.foreach { case (rank, metadata) =>
            push(rank, metadata)
            (0 until 5).foreach(_ => nop)
        }

        // start the test
        check_top

        // test the priority queue
        // test_ops.zipWithIndex.foreach { case (op, i) =>
        //     op match {
        //         case `op_nop` => nop
        //         case `op_push` => push(test_nums(i), i)
        //         case `op_pop` => pop
        //         case `op_replace` => replace(test_nums(i), i)
        //     }
        //     check_top
        // }

        var data_idx = 0

        test_ops.foreach { op =>
            op match {
                case `op_nop` =>
                    nop

                case `op_push` =>
                    check_top
                    push(test_nums(data_idx), data_idx)
                    data_idx += 1

                case `op_replace` =>
                    check_top
                    replace(test_nums(data_idx), data_idx)
                    data_idx += 1

                case `op_pop` =>
                    check_top
                    pop
            }
        }
    }
}