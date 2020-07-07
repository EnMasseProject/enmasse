MAKEDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
TOPDIR := $(MAKEDIR)/../..
CMD=access-control-server
include ../Makefile.go.mk

build_deps:
    # Nothing to do

run:
	NAMESPACE=$(NAMESPACE) \
	cd $(TOPDIR) && pwd && go run -mod=vendor cmd/access-control-server/main.go --developmentMode
