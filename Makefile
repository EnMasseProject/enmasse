PROJECT_NAME=qdrouterd
ROUTER_SOURCE_URL=http://github.com/apache/qpid-dispatch/archive/master.tar.gz

all: build

build:
	bash build_tarball ${ROUTER_SOURCE_URL}

clean:
	rm -rf proton_build proton_install qpid-dispatch.tar.gz qpid-dispatch-src qpid-proton.tar.gz qpid-proton-src staging build

.PHONY: build clean
