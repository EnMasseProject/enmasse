TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

build/$(CMD):
	cd $(GOPRJ)/cmd/$(@F) && GOOS=$(BUILD_GOOS) GOARCH=$(BUILD_GOARCH) go build -o $(abspath $@) .
ifneq ($(FULL_BUILD),true)
	mvn $(MAVEN_ARGS) package
endif

build: build/$(CMD)

build_go: build

package:
	$(IMAGE_ENV) mvn package -DskipTests $(MAVEN_ARGS)

test:

.PHONY: build/$(CMD) build
