all:
	if [ ! -f jsonnet ]; then curl -O http://pvv.ntnu.no/~lulf/jsonnet; fi
	chmod 755 jsonnet
	./jsonnet -V multicast=false -V persistence=false broker-template.jsonnet > queue-inmemory.json
	./jsonnet -V multicast=true -V persistence=false broker-template.jsonnet > topic-inmemory.json
