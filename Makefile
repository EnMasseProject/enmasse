SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))

all: prepare $(OBJS) yaml

%.json: %.jsonnet
	jsonnet/jsonnet -m generated $<

yaml:
	for i in generated/*.json; do ./convertyaml.rb $$i generated; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p generated
	cp include/*.json generated

clean:
	rm -rf generated
