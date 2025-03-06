// For more information on writing tests, see
// https://scalameta.org/munit/docs/getting-started.html
package ShiftRegisterPQ

import chisel3.util._;
import chisel3._;
import org.scalameta.util.AnyFlatSpec;


class ShiftRegisterPriorityQueueTest extends AnyFlatSpec with ChiselTestCase {
    behavior of "PriorityQueueBlock"
    it should "enqueue and dequeue new entries properly" in {
        test(new ShiftRegisterBlock()) {
            
        }
    }
}