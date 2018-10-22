TOPDIR=$(dir $(lastword $(MAKEFILE_LIST)))
BUILD_DIRS     = none-authservice
DOCKER_DIRS	   = agent topic-forwarder artemis api-server address-space-controller standard-controller keycloak-plugin keycloak-controller router router-metrics mqtt-gateway mqtt-lwt service-broker
FULL_BUILD 	   = true
DOCKER_REGISTRY ?= docker.io
OPENSHIFT_PROJECT             ?= $(shell oc project -q)
OPENSHIFT_USER                ?= $(shell oc whoami)
OPENSHIFT_TOKEN               ?= $(shell oc whoami -t)
OPENSHIFT_MASTER              ?= $(shell oc whoami --show-server=true)
OPENSHIFT_USE_TLS             ?= true
OPENSHIFT_REGISTER_API_SERVER ?= false

DOCKER_TARGETS = docker_build docker_tag docker_push clean
BUILD_TARGETS  = init build test package $(DOCKER_TARGETS) coverage
INSTALLDIR=$(CURDIR)/templates/install
SKIP_TESTS      ?= false

ifeq ($(SKIP_TESTS),true)
	MAVEN_ARGS="-DskipTests"
endif
ifneq ($(strip $(PROJECT_DISPLAY_NAME)),)
	MAVEN_ARGS+="-Dapplication.display.name=$(PROJECT_DISPLAY_NAME)"
endif


all: init build_java docker_build templates

templates: docu_html
	make -C templates

build_java:
	mvn package -q -B $(MAVEN_ARGS)

clean_java:
	mvn -B -q clean

template_clean:
	make -C templates clean

clean: clean_java docu_htmlclean template_clean

docker_build: build_java

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
	OPENSHIFT_PROJECT=$(OPENSHIFT_PROJECT) \
		OPENSHIFT_TOKEN=$(OPENSHIFT_TOKEN) \
		OPENSHIFT_USER=$(OPENSHIFT_USER) \
		OPENSHIFT_URL=$(OPENSHIFT_MASTER) \
		OPENSHIFT_USE_TLS=$(OPENSHIFT_USE_TLS) \
		REGISTER_API_SERVER=$(OPENSHIFT_REGISTER_API_SERVER) \
		./systemtests/scripts/run_tests.sh $(SYSTEMTEST_ARGS) $(SYSTEMTESTS_PROFILE)

client_install:
	./systemtests/scripts/client_install.sh


scripts/swagger2markup.jar:
	curl -o scripts/swagger2markup.jar https://repo.maven.apache.org/maven2/io/github/swagger2markup/swagger2markup-cli/1.3.3/swagger2markup-cli-1.3.3.jar

docu_swagger: scripts/swagger2markup.jar
	java -jar scripts/swagger2markup.jar convert -i api-server/src/main/resources/swagger.json -f documentation/common/restapi-reference

docu_html: docu_htmlclean docu_swagger docu_check
	mkdir -p documentation/html
	cp -vRL documentation/_images documentation/html/images
	mkdir -p documentation/html/resources
	cp -vRL documentation/common/*.yaml documentation/html/resources
	cp -vRL documentation/common/*.py documentation/html/resources
	asciidoctor -v --failure-level WARN -t -dbook documentation/master.adoc -o documentation/html/index.html
	asciidoctor -v --failure-level WARN -t -dbook documentation/contributing/master.adoc -o documentation/html/contributing.html

docu_htmlclean:
	rm -rf documentation/html

docu_check:
	./scripts/check_docs.sh


.PHONY: $(BUILD_TARGETS) $(DOCKER_TARGETS) $(BUILD_DIRS) $(DOCKER_DIRS) build_java systemtests clean_java docu_html docu_swagger docu_htmlclean docu_check
