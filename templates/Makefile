SRCS = $(wildcard *.jsonnet)
OBJS = $(patsubst %.jsonnet,%.json,$(SRCS))

all: fetch_tool $(OBJS)
	cp *.json ../

%.json: %.jsonnet
	./jsonnet $< -o $@

fetch_tool:
	if [ ! -f jsonnet ]; then curl -O http://pvv.ntnu.no/~lulf/jsonnet; fi
	chmod 755 jsonnet

clean:
	rm -rf *.json
