PACKAGE = fpga.pheap
MODULE = PHeap
WAVE_DIR = ./test_run_dir/$(MODULE)_should_work
VCD_FILE = $(WAVE_DIR)/$(MODULE).vcd

.PHONY: test vcd wave clean

test:
	@printf "test $(PACKAGE).$(MODULE)\nquit\n" | python builder.py

vcd:
	@printf "test $(PACKAGE).$(MODULE) -w\nquit\n" | python builder.py

wave: vcd
	gtkwave $(VCD_FILE)

clean:
	rm -rf ./test_run_dir/*