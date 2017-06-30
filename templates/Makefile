SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
TAG?=latest
INSTALLDIR=enmasse-$(TAG)
DOCKER_ORG?=enmasseproject
DOCKER_REGISTRY?=docker.io

all: prepare $(OBJS) yaml

%.json: %.jsonnet
	VERSION=$(TAG) REGISTRY=$(DOCKER_REGISTRY) PROJECT=$(DOCKER_ORG) jsonnet/jsonnet --ext-str VERSION --ext-str REGISTRY --ext-str PROJECT -m generated $<

yaml:
	for d in kubernetes openshift; do for i in `find generated/$$d -name "*.json"`; do b=`dirname $$i`; o="install/$${b#generated/}"; mkdir -p $$o; ./scripts/convertyaml.rb $$i $$o; done; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p generated/kubernetes/addons
	mkdir -p generated/openshift/addons
	cp include/*.json generated

clean:
	rm -rf generated $(INSTALLDIR) $(INSTALLDIR).tar.gz

test:
	@echo $(TAG)
