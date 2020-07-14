TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

GO              ?= go
GOOPTS          ?= -mod=vendor

build/$(CMD): build_deps
	GO111MODULE=on CGO_ENABLED=0 GOOS=$(BUILD_GOOS) GOARCH=$(BUILD_GOARCH) $(GO) build $(GOOPTS) -o $(abspath $@) $(abspath $(TOPDIR)/cmd/$(@F))
ifneq ($(FULL_BUILD),true)
	mvn $(MAVEN_ARGS) package
endif

build: build/$(CMD)

build_go: build

deploy:
	$(IMAGE_ENV) mvn -Prelease deploy $(MAVEN_ARGS)

package:
	$(IMAGE_ENV) mvn package -DskipTests $(MAVEN_ARGS)

test:

.PHONY: build/$(CMD) build
