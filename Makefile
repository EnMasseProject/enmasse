SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
TAG?=latest

all: prepare $(OBJS) yaml
	tar -czf enmasse-${TAG}.tar.gz install/kubernetes install/openshift scripts/common.sh scripts/deploy-kubernetes.sh scripts/deploy-openshift.sh

%.json: %.jsonnet
	VERSION=$(TAG) jsonnet/jsonnet --ext-str VERSION -m generated $<

yaml:
	for d in kubernetes openshift; do for i in `find generated/$$d -name "*.json"`; do b=`dirname $$i`; o="install/$${b#generated/}"; mkdir -p $$o; ./scripts/convertyaml.rb $$i $$o; done; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p generated/kubernetes/addons
	mkdir -p generated/openshift/addons
	cp include/*.json generated

clean:
	rm -rf generated

test:
	@echo $(TAG)
