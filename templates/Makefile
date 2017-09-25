include ../Makefile.common

SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
INSTALLDIR=enmasse-$(VERSION)

%.json: %.jsonnet
	VERSION=$(TAG) REGISTRY=$(DOCKER_REGISTRY) PROJECT=$(DOCKER_ORG) jsonnet/jsonnet --ext-str VERSION --ext-str REGISTRY --ext-str PROJECT -m build $<

yaml:
	for d in kubernetes openshift; do for i in `find build/$$d -name "*.json"`; do b=`dirname $$i`; o="install/$${b#build/}"; mkdir -p $$o; ./scripts/convertyaml.py $$i $$o; done; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p build/kubernetes/addons
	mkdir -p build/openshift/addons
	cp include/*.json build 

package: prepare $(OBJS) yaml
	mkdir -p build/$(INSTALLDIR)                                                                                                                                               
	cp -r install/* build/$(INSTALLDIR)                                                                                                                                        
	tar -czf build/$(INSTALLDIR).tgz -C build $(INSTALLDIR)

coverage:

.PHONY: yaml prepare package clean
