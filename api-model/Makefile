include ../Makefile.java.mk

build_deps::
	mkdir -p build
	go build -o build/generate generator/generate.go
	./build/generate > src/main/resources/schema/kube-schema.json

.PHONY: build_deps
