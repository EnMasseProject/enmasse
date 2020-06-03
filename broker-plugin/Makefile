include ../Makefile.java.mk
GOOPTS          ?= -mod=vendor
CMD = broker-probe


deploy:
	@echo "Skipping deploy"

build_go: build/$(CMD)

build_deps:: build/$(CMD)

build/$(CMD):
	GO111MODULE=on GOOS=$(BUILD_GOOS) GOARCH=$(BUILD_GOARCH) go build $(GOOPTS) -o $(abspath $@) $(abspath $(TOPDIR)/cmd/$(@F))
