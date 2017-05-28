SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
TRAVIS_TAG ?= "latest"

all: prepare $(OBJS) yaml

%.json: %.jsonnet
	VERSION=$(TRAVIS_TAG) jsonnet/jsonnet --ext-str VERSION -m generated $<

yaml:
	for d in kubernetes openshift; do for i in `find generated/$$d -name "*.json"`; do b=`dirname $$i`; o="install/$${b#generated/}"; mkdir -p $$o; ./scripts/convertyaml.rb $$i $$o; done; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p generated/kubernetes/addons
	mkdir -p generated/openshift/addons
	cp include/*.json generated

clean:
	rm -rf generated install

test:
	@echo $(TRAVIS_TAG)
