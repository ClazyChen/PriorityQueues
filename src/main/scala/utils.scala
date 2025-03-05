import chisel3._

object Functions {
    // >
    def cmpGreater(a: UInt, b: UInt): Bool = {
        return (b -& a)(a.getWidth)
    }
}