include ../Makefile.java.mk

GO              ?= go
GOOPTS          ?= -mod=vendor
CMD = broker-probe

deploy:
	@echo "Skipping deploy"

build_go: build/$(CMD)

build_deps:: build/$(CMD)

build/$(CMD):
	GO111MODULE=on GOOS=$(BUILD_GOOS) GOARCH=$(BUILD_GOARCH) $(GO) build $(GOOPTS) -o $(abspath $@) $(abspath $(TOPDIR)/cmd/$(@F))
