TOPDIR          := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
include $(TOPDIR)/Makefile.env.mk

BUILD_DIRS       = none-authservice iot/iot-operator iot/qdr-proxy-configurator iot/iot-gc
DOCKER_DIRS      = agent topic-forwarder artemis broker-plugin api-server address-space-controller standard-controller keycloak-plugin keycloak-controller router router-metrics mqtt-gateway mqtt-lwt service-broker
FULL_BUILD       = true

DOCKER_TARGETS   = docker_build docker_tag docker_push clean
BUILD_TARGETS    = init build test package $(DOCKER_TARGETS) coverage
INSTALLDIR       = $(CURDIR)/templates/install
SKIP_TESTS      ?= false

ASCIIDOCTOR_EXTRA_FLAGS = --failure-level WARN
ASCIIDOCTOR_FLAGS       = -v -a EnMasseVersion=$(VERSION) -t -dbook $(ASCIIDOCTOR_EXTRA_FLAGS)

ifeq ($(SKIP_TESTS),true)
	MAVEN_ARGS="-DskipTests"
endif
ifneq ($(strip $(PROJECT_DISPLAY_NAME)),)
	MAVEN_ARGS+="-Dapplication.display.name=$(PROJECT_DISPLAY_NAME)"
endif

all: init build_java docker_build templates

templates: docu_html
	$(MAKE) -C templates

build_java:
	$(IMAGE_ENV) mvn package -q -B $(MAVEN_ARGS)

test_go: test_go_vet test_go_run

test_go_vet:
	go tool vet cmd pkg

ifeq (,$(GO2XUNIT))
test_go_run:
	go test -v ./...
else
test_go_run:
	mkdir -p build
	go test -v ./... 2>&1 | tee $(abspath build/go.testoutput)
	$(GO2XUNIT) -fail -input build/go.testoutput -output build/TEST-go.xml
endif

coverage_go:
	go test -cover ./...

buildpush:
	$(MAKE)
	$(MAKE) docker_tag
	$(MAKE) docker_push

clean_java:
	mvn -B -q clean

template_clean:
	$(MAKE) -C templates clean

clean: clean_java clean_go docu_htmlclean template_clean
	rm -rf build

docker_build: build_java test_go

coverage: java_coverage
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ coverage

java_coverage:
	mvn test -Pcoverage -B $(MAVEN_ARGS)
	mvn jacoco:report-aggregate

$(BUILD_TARGETS): $(BUILD_DIRS)
$(BUILD_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

$(DOCKER_TARGETS): $(DOCKER_DIRS)
$(DOCKER_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

systemtests:
	make -C systemtests

scripts/swagger2markup.jar:
	curl -o scripts/swagger2markup.jar https://repo.maven.apache.org/maven2/io/github/swagger2markup/swagger2markup-cli/1.3.3/swagger2markup-cli-1.3.3.jar

docu_swagger: scripts/swagger2markup.jar
	java -jar scripts/swagger2markup.jar convert -i api-server/src/main/resources/swagger.json -f documentation/common/restapi-reference

docu_html: docu_htmlclean docu_swagger docu_check
	mkdir -p documentation/html
	mkdir -p documentation/html/kubernetes
	mkdir -p documentation/html/openshift
	cp -vRL documentation/_images documentation/html/kubernetes/images
	cp -vRL documentation/_images documentation/html/openshift/images
	asciidoctor $(ASCIIDOCTOR_FLAGS) documentation/master-kubernetes.adoc -o documentation/html/kubernetes/index.html
	asciidoctor $(ASCIIDOCTOR_FLAGS) documentation/master-openshift.adoc -o documentation/html/openshift/index.html
	asciidoctor $(ASCIIDOCTOR_FLAGS) documentation/contributing/master.adoc -o documentation/html/contributing.html

docu_htmlclean:
	rm -rf documentation/html

docu_check:
	./scripts/check_docs.sh

docu_clean: docu_htmlclean
	rm scripts/swagger2markup.jar

.PHONY: all $(BUILD_TARGETS) $(DOCKER_TARGETS) $(BUILD_DIRS) $(DOCKER_DIRS) build_java test_go systemtests clean_java docu_html docu_swagger docu_htmlclean docu_check test_go_vet test_go_plain
