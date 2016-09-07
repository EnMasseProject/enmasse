SRCS = $(wildcard *.jsonnet)
OBJS = $(patsubst %.jsonnet,%.json,$(SRCS))

all: prepare $(OBJS)

%.json: %.jsonnet
	./jsonnet -m generated $<

prepare:
	if [ ! -f jsonnet ]; then curl -O http://pvv.ntnu.no/~lulf/jsonnet; fi
	chmod 755 jsonnet
	mkdir -p generated

clean:
	rm -rf generated
