SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))

all: prepare $(OBJS) yaml
	mv generated/*queue*.yaml ../
	mv generated/*topic*.yaml ../
	mv generated/*-template.yaml ../
	mv generated/addresses.yaml ../
	mv generated/flavor.yaml ../

%.json: %.jsonnet
	./jsonnet -m generated $<

yaml:
	for i in generated/*.json; do ./convertyaml.rb $$i generated; done

prepare:
	if [ ! -f jsonnet ]; then curl -O http://pvv.ntnu.no/~lulf/jsonnet; fi
	chmod 755 jsonnet
	mkdir -p generated
	cp include/*.json generated

clean:
	rm -rf generated
