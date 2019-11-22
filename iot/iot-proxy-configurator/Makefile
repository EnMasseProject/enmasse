CMD=iot-proxy-configurator
include ../../Makefile.go.mk

build_deps: Dockerfile

Dockerfile:
	cat Dockerfile.template | sed -e 's,\$${ROUTER_IMAGE},$(ROUTER_IMAGE),g' > Dockerfile

.PHONY: Dockerfile
