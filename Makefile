TOPDIR          := $(dir $(lastword $(MAKEFILE_LIST)))
include $(TOPDIR)/Makefile.env.mk

GO_DIRS = \
	controller-manager \
	iot/iot-proxy-configurator \
	console/console-server \
	access-control-server \
	broker-plugin

DOCKER_DIRS = \
	console/console-init \
	olm-manifest \
	iot/iot-tenant-service \
	iot/iot-auth-service \
	iot/iot-tenant-cleaner \
	iot/images/iot-adapters \
	iot/images/iot-device-registry \


FULL_BUILD       = true
GOOPTS          ?= -mod=vendor

DOCKER_TARGETS   = docker_build docker_tag docker_push kind_load_image clean
INSTALLDIR       = $(CURDIR)/templates/install

SKIP_TESTS          ?= false
SKIP_VERIFY_CODEGEN ?= false
SKIP_MANIFESTS      ?= false
MAVEN_BATCH         ?= true

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

templates: imageenv manifests
	$(MAKE) -C templates

deploy: build_go
	$(IMAGE_ENV) IMAGE_ENV="$(IMAGE_ENV)" mvn -Prelease deploy $(MAVEN_ARGS)

build_java: build_go templates
	$(IMAGE_ENV) IMAGE_ENV="$(IMAGE_ENV)" mvn package -q $(MAVEN_ARGS)

build_go: $(GO_DIRS) test_go

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
ifeq ($(SKIP_VERIFY_CODEGEN),false)
	GO111MODULE=on ./hack/verify-codegen.sh
endif

test_go_vet:
	GO111MODULE=on time go vet $(GOOPTS) ./cmd/... ./pkg/...

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
	time $(MAKE)
	time $(MAKE) docker_build
	time $(MAKE) -j 4 docker_tag
	time $(MAKE) -j 4 docker_push

buildpushkind:
	time $(MAKE)
	time $(MAKE) docker_build
	time $(MAKE) -j 4 docker_tag
	time $(MAKE) kind_load_image

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

#region Targets related to kubebuilder

ifeq (, $(shell which controller-gen 2>/dev/null))

LOCALBIN:=$(TOPDIR)/local/go/bin
CONTROLLER_GEN:=$(abspath $(LOCALBIN)/controller-gen )
controller-gen: $(CONTROLLER_GEN)

$(CONTROLLER_GEN):
	@mkdir -p "$(LOCALBIN)"
	@{ \
	set -e ;\
	CONTROLLER_GEN_TMP_DIR=$$(mktemp -d) ;\
	cd $$CONTROLLER_GEN_TMP_DIR ;\
	go mod init tmp ;\
	GOPATH=$(abspath $(LOCALBIN)/../) go get sigs.k8s.io/controller-tools/cmd/controller-gen@v0.3.0 ;\
	rm -rf $$CONTROLLER_GEN_TMP_DIR ;\
	}

else

controller-gen:
.PHONY: controller-gen
CONTROLLER_GEN:=$(shell which controller-gen)

endif

ifeq ($(SKIP_MANIFESTS),true)
manifests:
	@echo "Skipping generating manifests from source"
else
manifests: controller-gen
	$(CONTROLLER_GEN) crd paths=./pkg/apis/enmasse/v1 output:dir=./templates/crds
endif

#endregion

.PHONY: test_go_vet test_go_plain build_go imageenv buildpushkind
.PHONY: all $(GO_DIRS) $(DOCKER_TARGETS) $(DOCKER_DIRS) build_java test_go systemtests clean_java docu_html docu_check docu_clean templates
.PHONY: manifests
