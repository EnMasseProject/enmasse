TOPDIR          := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))/..
include ../Makefile.env.mk

ASCIIDOCTOR_EXTRA_FLAGS ?= --failure-level WARN
ASCIIDOCTOR_FLAGS       ?= -v -a EnMasseVersion=$(VERSION) -t -dbook $(ASCIIDOCTOR_EXTRA_FLAGS)

.PHONY: swagger
swagger: build/swagger2markup.jar
	mvn process-resources
	java -jar build/swagger2markup.jar convert -i target/classes/swagger.json -f common/restapi-reference

build/swagger2markup.jar:
	mkdir -p build
	curl -o build/swagger2markup.jar https://repo.maven.apache.org/maven2/io/github/swagger2markup/swagger2markup-cli/1.3.3/swagger2markup-cli-1.3.3.jar

.PHONY: build
build: swagger check
	mkdir -p html
	mkdir -p html/kubernetes
	mkdir -p html/openshift
	cp -vRL _images html/kubernetes/images
	cp -vRL _images html/openshift/images
	asciidoctor $(ASCIIDOCTOR_FLAGS) master-kubernetes.adoc -o html/kubernetes/index.html
	asciidoctor $(ASCIIDOCTOR_FLAGS) master-openshift.adoc -o html/openshift/index.html
	asciidoctor $(ASCIIDOCTOR_FLAGS) contributing/master.adoc -o html/contributing.html

ifdef PUBLISH_DEST
.PHONY: publish
publish: build
	rsync -av html/* $(PUBLISH_DEST)/
else
.PHONY: publish
publish:
	@echo "PUBLISH_DEST not set"
endif

.PHONY: clean
clean:
	rm -rf html build

.PHONY: check
check:
	$(TOPDIR)/scripts/check_docs.sh
