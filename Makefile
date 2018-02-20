TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
BUILD_DIRS     = agent none-authservice templates
DOCKER_DIRS	   = topic-forwarder artemis address-controller standard-controller keycloak-plugin keycloak-controller router router-metrics mqtt-gateway mqtt-lwt
FULL_BUILD 	   = true
DOCKER_REGISTRY ?= docker.io
OPENSHIFT_PROJECT ?= $(shell oc project -q)
OPENSHIFT_USER    ?= $(shell oc whoami)
OPENSHIFT_TOKEN   ?= $(shell oc whoami -t)
OPENSHIFT_MASTER  ?= $(shell oc whoami --show-server=true)

DOCKER_TARGETS = docker_build docker_tag docker_push clean
BUILD_TARGETS  = init build test package $(DOCKER_TARGETS) coverage
INSTALLDIR=$(CURDIR)/templates/install
SKIP_TESTS      ?= false

ifeq ($(SKIP_TESTS),true)
	MAVEN_ARGS="-DskipTests"
endif

all: init build test package docker_build

build_java:
	mvn -q test package -B $(MAVEN_ARGS)

clean_java:
	mvn -B clean

clean: clean_java

docker_build: build_java

coverage: java_coverage
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ coverage

java_coverage: build_amqp_module
	mvn test -Pcoverage package -B $(MAVEN_ARGS)
	mvn jacoco:report-aggregate

$(BUILD_TARGETS): $(BUILD_DIRS)
$(BUILD_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

$(DOCKER_TARGETS): $(DOCKER_DIRS)
$(DOCKER_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

deploy:
	./templates/install/deploy-openshift.sh -n $(OPENSHIFT_PROJECT) -u $(OPENSHIFT_USER) -m $(OPENSHIFT_MASTER) -o multitenant -a "standard none"

systemtests:
	OPENSHIFT_PROJECT=$(OPENSHIFT_PROJECT) OPENSHIFT_TOKEN=$(OPENSHIFT_TOKEN) OPENSHIFT_USER=$(OPENSHIFT_USER) OPENSHIFT_URL=$(OPENSHIFT_MASTER) OPENSHIFT_USE_TLS=true ./systemtests/scripts/run_tests.sh $(SYSTEMTEST_ARGS)

client_install:
	./systemtests/scripts/client_install.sh

webdriver_install:
	./systemtests/scripts/install_web_drivers.sh

.PHONY: $(BUILD_TARGETS) $(DOCKER_TARGETS) $(BUILD_DIRS) $(DOCKER_DIRS) build_java deploy systemtests clean_java
