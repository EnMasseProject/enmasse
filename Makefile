BUILD_DIRS     = ragent subserv console none-authservice templates
DOCKER_DIRS	   = topic-forwarder artemis address-controller queue-scheduler configserv keycloak keycloak-controller router router-metrics mqtt-gateway mqtt-lwt
FULL_BUILD 	   = true
DOCKER_REGISTRY ?= docker.io
OPENSHIFT_PROJECT ?= $(shell oc project -q)
OPENSHIFT_USER    ?= $(shell oc whoami)
OPENSHIFT_TOKEN   ?= $(shell oc whoami -t)
OPENSHIFT_MASTER  ?= $(shell oc whoami --show-server=true)
MULTITENANT       ?= false

DOCKER_TARGETS = docker_build docker_tag docker_push
BUILD_TARGETS  = init build test package clean $(DOCKER_TARGETS) coverage

all: init build test package docker_build

build_java:
	./gradlew build $(GRADLE_ARGS)
	./gradlew pack --rerun-tasks $(GRADLE_ARGS)

clean_java:
	./gradlew clean

clean: clean_java

docker_build: build_java

$(BUILD_TARGETS): $(BUILD_DIRS)
$(BUILD_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

$(DOCKER_TARGETS): $(DOCKER_DIRS)
$(DOCKER_DIRS):
	$(MAKE) FULL_BUILD=$(FULL_BUILD) -C $@ $(MAKECMDGOALS)

deploy:
	./templates/install/deploy-openshift.sh -n $(OPENSHIFT_PROJECT) -u $(OPENSHIFT_USER) -m $(OPENSHIFT_MASTER) -p MULTITENANT=$(MULTITENANT) -a "standard none"

systemtests:
	OPENSHIFT_PROJECT=$(OPENSHIFT_PROJECT) OPENSHIFT_MULTITENANT=$(MULTITENANT) OPENSHIFT_TOKEN=$(OPENSHIFT_TOKEN) OPENSHIFT_USER=$(OPENSHIFT_USER) OPENSHIFT_URL=$(OPENSHIFT_MASTER) OPENSHIFT_USE_TLS=true ./gradlew :systemtests:test -Psystemtests -i --rerun-tasks $(GRADLE_ARGS)


.PHONY: $(BUILD_TARGETS) $(DOCKER_TARGETS) $(BUILD_DIRS) $(DOCKER_DIRS) build_java deploy systemtests clean_java
