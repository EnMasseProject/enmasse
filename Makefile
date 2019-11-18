TOPDIR          := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
include $(TOPDIR)/Makefile.env.mk

GO_DIRS = \
	controller-manager \
	iot/iot-proxy-configurator \

DOCKER_DIRS = \
	agent \
	topic-forwarder \
	broker-plugin \
	api-server \
	address-space-controller \
	none-authservice \
	standard-controller \
	keycloak-plugin \
	mqtt-gateway \
	mqtt-lwt \
	service-broker \
	console/console-init \
	console/console-httpd \
	olm-manifest \
	iot/iot-tenant-service \
	iot/iot-auth-service \
	iot/iot-device-registry-file \
	iot/iot-device-registry-infinispan \
	iot/iot-http-adapter \
	iot/iot-mqtt-adapter \
	iot/iot-lorawan-adapter \
	iot/iot-sigfox-adapter \

FULL_BUILD       = true


DOCKER_TARGETS   = docker_build docker_tag docker_push clean
INSTALLDIR       = $(CURDIR)/templates/install
SKIP_TESTS      ?= false

ifeq ($(SKIP_TESTS),true)
	MAVEN_ARGS=-DskipTests -Dmaven.test.skip=true
endif

all: build_java build_go templates

templates: imageenv
	$(MAKE) -C templates

deploy: build_go
	$(IMAGE_ENV) mvn -Prelease deploy $(MAVEN_ARGS)

build_java: build_go
	$(IMAGE_ENV) mvn package -q -B $(MAVEN_ARGS)

build_go: $(GO_DIRS) test_go

imageenv:
	@echo $(IMAGE_ENV) > imageenv.txt

imagelist:
	@echo $(IMAGE_LIST) > imagelist.txt

$(GO_DIRS): $(GOPRJ)
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

ifeq ($(SKIP_TESTS),true)
test_go:
else
test_go: test_go_vet test_go_run
endif

test_go_vet:
	cd $(GOPRJ) && go vet -mod=vendor ./cmd/... ./pkg/...

ifeq (,$(GO2XUNIT))
test_go_run: $(GOPRJ)
	cd $(GOPRJ) && go test -mod=vendor -v ./...
else
test_go_run: $(GOPRJ)
	mkdir -p build
	-cd $(GOPRJ) && go test -mod=vendor -v ./... 2>&1 | tee $(abspath build/go.testoutput)
	$(GO2XUNIT) -fail -input build/go.testoutput -output build/TEST-go.xml
endif

coverage_go:
	cd $(GOPRJ) && go test -mod=vendor -cover ./...

buildpush:
	$(MAKE)
	$(MAKE) docker_build
	$(MAKE) docker_tag
	$(MAKE) docker_push

$(GOPRJ):
	mkdir -p $(dir $(GOPRJ))
	ln -s $(TOPDIR) $(GOPRJ)

clean_go:
	@rm -Rf $(GOPATH)

clean_java:
	mvn -B -q clean

template_clean:
	$(MAKE) -C templates clean

clean: clean_java clean_go docu_clean template_clean
	rm -rf build

coverage: java_coverage
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ coverage

java_coverage:
	mvn test -Pcoverage -B $(MAVEN_ARGS)
	mvn jacoco:report-aggregate

$(DOCKER_TARGETS): $(DOCKER_DIRS) $(GO_DIRS)
$(DOCKER_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

systemtests:
	make -C systemtests

docu_html:
	make -C documentation build

docu_check:
	make -C documentation check

docu_clean:
	make -C documentation clean

.PHONY: test_go_vet test_go_plain build_go imageenv
.PHONY: all $(GO_DIRS) $(DOCKER_TARGETS) $(DOCKER_DIRS) build_java test_go systemtests clean_java docu_html docu_check docu_clean templates
