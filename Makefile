TOPDIR          := $(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.env.mk

GO_DIRS = \
	controller-manager \
	console/console-server \
	broker-plugin

DOCKER_DIRS = \
	agent \
	topic-forwarder \
	address-space-controller \
	none-authservice \
	standard-controller \
	keycloak-plugin \
	service-broker \
	console/console-init \
	olm-manifest

FULL_BUILD       = true
GOOPTS          ?= -mod=vendor

DOCKER_TARGETS   = docker_build docker_tag docker_push kind_load_image clean
INSTALLDIR       = $(CURDIR)/templates/install
SKIP_TESTS      ?= false
MAVEN_BATCH     ?= true

ifndef GOPATH
	GOPATH=/tmp/go
endif

ifeq ($(SKIP_TESTS),true)
	MAVEN_ARGS+=-DskipTests -Dmaven.test.skip=true
endif
ifeq ($(MAVEN_BATCH),true)
	MAVEN_ARGS+=-B
endif

all: build_java build_go templates

templates: imageenv
	$(MAKE) -C templates

deploy: build_go
	$(IMAGE_ENV) IMAGE_ENV="$(IMAGE_ENV)" mvn -Prelease deploy $(MAVEN_ARGS)

build_java: build_go templates
	$(IMAGE_ENV) IMAGE_ENV="$(IMAGE_ENV)" mvn package $(MAVEN_ARGS)

build_go: $(GO_DIRS) test_go

olmindex:
	$(MAKE) -C olm-index docker_build docker_tag

olmindex_push:
	$(MAKE) -C olm-index docker_push

imageenv:
	@echo $(IMAGE_ENV) > imageenv.txt

imagelist:
	@echo $(IMAGE_LIST) > imagelist.txt

$(GO_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

ifeq ($(SKIP_TESTS),true)
test_go:
else
test_go: test_go_vet test_go_codegen test_go_run
endif

test_go_codegen:
	GO111MODULE=on ./hack/verify-codegen.sh

test_go_vet:
	GO111MODULE=on go vet $(GOOPTS) ./cmd/... ./pkg/...

ifeq (,$(GO2XUNIT))
test_go_run:
	GO111MODULE=on go test $(GOOPTS) -v ./...
else
test_go_run:
	mkdir -p build
	GO111MODULE=on go test $(GOOPTS) -v ./... 2>&1 | tee $(abspath build/go.testoutput)
	$(GO2XUNIT) -fail -input build/go.testoutput -output build/TEST-go.xml
endif

coverage_go:
	GO111MODULE=on go test $(GOOPTS) -cover ./...

buildpush:
	$(MAKE)
	$(MAKE) docker_build
	$(MAKE) docker_tag
	$(MAKE) docker_push

buildpushkind:
	$(MAKE)
	$(MAKE) docker_build
	$(MAKE) docker_tag
	$(MAKE) kind_load_image

clean_java:
	mvn -q clean $(MAVEN_ARGS)

template_clean:
	$(MAKE) -C templates clean

clean: clean_java clean_go docu_clean template_clean
	rm -rf build

clean_go:
	rm -Rf go-bin

coverage: java_coverage
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ coverage

java_coverage:
	mvn test -Pcoverage $(MAVEN_ARGS)
	mvn jacoco:report-aggregate $(MAVEN_ARGS)

$(DOCKER_TARGETS): $(DOCKER_DIRS) $(GO_DIRS)
$(DOCKER_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

systemtests:
	make -C systemtests

systemtests-tekton:
	make -C systemtests systemtests-tekton

docu_html:
	make -C documentation

docu_check:
	make -C documentation check

docu_clean:
	make -C documentation clean

.PHONY: test_go_vet test_go_plain build_go imageenv buildpushkind
.PHONY: all $(GO_DIRS) $(DOCKER_TARGETS) $(DOCKER_DIRS) build_java test_go systemtests clean_java docu_html docu_check docu_clean templates
