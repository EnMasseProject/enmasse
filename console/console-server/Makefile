MAKEDIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
TOPDIR := $(MAKEDIR)/../..
CMD=console-server
include ../../Makefile.go.mk

build_deps:
    # Nothing to do

run:
	ENMASSE_ADDRESS_SPACE_TYPE_FILE=$(MAKEDIR)/src/main/resources/addressSpaceTypes.json \
	ENMASSE_ADDRESS_TYPE_FILE=$(MAKEDIR)/src/main/resources/addressTypes.json \
	NAMESPACE=$(NAMESPACE) \
	cd $(TOPDIR) && pwd && go run -mod=vendor cmd/console-server/main.go --developmentMode
