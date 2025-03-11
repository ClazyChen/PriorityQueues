# How to use builder.py

For example, for `fpga.sr.ShiftRegister`:

In CLI, `python builder.py` first.

- to generate the system verilog file:
```
build fpga.sr.ShiftRegister
```
then `builder.py` will automatically generate a `Main.scala` and run `sbt runMain` to generate a system verilog file.

- to apply the black box test (you can modify `src/test/scala/fpga/BlackBox.scala` if required):
```
test fpga.sr.ShiftRegister
```
then `builder.py` will automatically generate a `ShiftRegisterTest.scala` and run `sbt testOnly` to apply the black box test. The results can be found in `test_run_dir`.

- to use the other functions provided by `builder.py`, try `help` or just read the source code.

