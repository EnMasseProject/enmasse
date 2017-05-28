SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
TRAVIS_TAG ?= "latest"

all: prepare $(OBJS) yaml
	mv generated/*.yaml install/

%.json: %.jsonnet
	VERSION=$(TRAVIS_TAG) jsonnet/jsonnet --ext-str VERSION -m generated $<

yaml:
	for i in generated/*.json; do ./scripts/convertyaml.rb $$i generated; done
	ls generated/*.yaml

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p generated
	mkdir -p install
	cp include/*.json generated

clean:
	rm -rf generated

test:
	@echo $(TRAVIS_TAG)
