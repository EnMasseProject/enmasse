SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
VERSION="latest"
ifdef $(TRAVIS_TAG)
VERSION=$(TRAVIS_TAG)
endif

all: prepare $(OBJS) yaml

%.json: %.jsonnet
	VERSION=$(VERSION) jsonnet/jsonnet --ext-str VERSION -m generated $<

yaml:
	for i in generated/*.json; do ./convertyaml.rb $$i generated; done
	ls generated/*.yaml

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p generated
	cp include/*.json generated

clean:
	rm -rf generated

test:
	@echo $(VERSION)
