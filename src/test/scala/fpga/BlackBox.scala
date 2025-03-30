package fpga

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.util.Random
import scala.collection.mutable.PriorityQueue
import fpga.Const._

// a black box for the priority queue
object BlackBox {

    // cold start operations (all of them are push operations)
    val cold_start_ops = 10

    // number of operations in the test
    val num_ops = 100

    // push, pop, replace ratio
    // val ratio = (1.0, 0.0, 0.0)
    // val ratio = (0.6, 0.4, 0.0)
    val ratio = (0.3, 0.2, 0.5)
    val op_push = 0
    val op_pop = 1
    val op_replace = 2

    def to_op(r: Double): Int = {
        if (r < ratio._1) op_push
        else if (r < ratio._1 + ratio._2) op_pop
        else op_replace
    }

    // random seed
    val seed = 1234567890

    def debugPrint[PQ <: PriorityQueueTrait](tag: String)(implicit pq: PQ): Unit = {
        val entries = pq.io.dbg_port.get.map(_.rank.peek().litValue)
        println(f"${tag}%-8s: ${entries.mkString("[", ", ", "]")}")
        val temps = pq.io.dbg_port1.get.map(_.rank.peek().litValue)
        println(f"${"temp"}%-8s: ${temps.mkString("[", ", ", "]")}")
    }

    def idle[PQ <: PriorityQueueTrait](implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
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
        if(debug) debugPrint(s"push(${rank})")
    }

    // pop the top entry from the priority queue
    def pop[PQ <: PriorityQueueTrait](implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        pq.io.op_in.push.existing.poke(false.B)
        pq.io.op_in.push.rank.poke(-1.S(rank_width.W).asUInt)
        pq.io.op_in.push.metadata.poke(0.U)
        pq.io.op_in.pop.poke(true.B)
        pq.clock.step()
        // if(debug) debugPrint("pop1")
        idle
        if(debug) debugPrint("pop")
        std_pq.dequeue()
    }

    // replace the top entry with a new entry
    def replace[PQ <: PriorityQueueTrait](rank: Int, metadata: Int)(implicit pq: PQ, std_pq: PriorityQueue[(Int, Int)]): Unit = {
        pq.io.op_in.push.existing.poke(true.B)
        pq.io.op_in.push.rank.poke(rank.U)
        pq.io.op_in.push.metadata.poke(metadata.U)
        pq.io.op_in.pop.poke(true.B)
        pq.clock.step()
        idle
        std_pq.enqueue((rank, metadata))
        std_pq.dequeue()
        if(debug) debugPrint(s"rep(${rank})")
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

    // the test body
    def test_black_box[PQ <: PriorityQueueTrait](c: PQ): Unit = {
        // random generator in [0, 2^rank_width - 1]
        val random = new Random(seed)

        // generate the cold start numbers and the test numbers
        val cold_start_nums = Array.fill(cold_start_ops)(random.nextInt(1 << rank_width))
        val test_nums = Array.fill(num_ops)(random.nextInt(1 << rank_width))

        // test operations, the ratio is recorded in the ratio variable
        // 0 for push, 1 for pop, 2 for replace
        val test_ops = Array.fill(num_ops)(to_op(random.nextDouble()))

        // the built-in priority queue to check the result, and the priority queue to test
        lazy implicit val std_pq = new PriorityQueue[(Int, Int)]()(Ordering.by((x: (Int, Int)) => (x._1, x._2)).reverse)
        lazy implicit val pq = c

        // initialize the priority queue
        cold_start_nums.zipWithIndex.foreach { case (rank, metadata) =>
            push(rank, metadata)
        }

        // start the test
        check_top

        // test the priority queue
        test_ops.zipWithIndex.foreach { case (op, i) =>
            op match {
                case `op_push` => push(test_nums(i), i)
                case `op_pop` => pop
                case `op_replace` => replace(test_nums(i), i)
            }
            check_top
        }

    }
}
