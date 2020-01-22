TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.common

build/$(CMD): build_deps
	GO111MODULE=on GOOS=$(BUILD_GOOS) GOARCH=$(BUILD_GOARCH) go build -mod=vendor -o $(abspath $@) .
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
