TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common
CMD_DIR=$(TOPDIR)/cmd

build:
	go build -o build/$(CMD) $(CMD_DIR)/$(CMD)
