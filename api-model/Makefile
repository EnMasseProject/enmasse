include ../Makefile.java.mk

build_deps: generatejava

generatejava:
	mkdir -p build
	go build -o build/generate generator/generate.go
	./build/generate > src/main/resources/schema/kube-schema.json

.PHONY: generatejava
