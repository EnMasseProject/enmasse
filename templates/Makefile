all:
	if [ ! -f jsonnet ]; then curl -O http://pvv.ntnu.no/~lulf/jsonnet; fi
	chmod 755 jsonnet
	./jsonnet -V multicast=false -V persistence=false -V secure=false broker-template.jsonnet > ../queue-inmemory.json
	./jsonnet -V multicast=true -V persistence=false -V secure=false broker-template.jsonnet > ../topic-inmemory.json
	./jsonnet -V multicast=false -V persistence=false -V secure=true broker-template.jsonnet > ../secure-queue-inmemory.json
	./jsonnet -V multicast=true -V persistence=false -V secure=true broker-template.jsonnet > ../secure-topic-inmemory.json
	./jsonnet -V secure=false enmasse-template.jsonnet > ../enmasse-template.json
	./jsonnet -V secure=true enmasse-template.jsonnet > ../secure-enmasse-template.json
