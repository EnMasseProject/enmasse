PROJECT_NAME=artemis
JARDIR=extra-jars

include ../Makefile.common

fetch_source:
	rm -rf $(JARDIR)
	$(FETCH_SOURCE) http://www.pvv.org/~lulf/apache-artemis-1.4.0-20160620.113416-8-bin.tar.gz
	cd plugin && gradle build
	tar xvf plugin/build/distributions/plugin.tar -C /tmp
	mkdir -p $(JARDIR)
	cp -r /tmp/plugin/lib/plugin.jar $(JARDIR)
	cp -r /tmp/plugin/lib/kubernetes-0.9.0.jar $(JARDIR)
	cp -r /tmp/plugin/lib/common-0.9.0.jar $(JARDIR)
	cp -r /tmp/plugin/lib/jboss-dmr-1.3.0.Final.jar $(JARDIR)
