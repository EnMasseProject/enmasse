include ../Makefile.java.mk
ARTIFACT_BASE=build/artemis-image
ARTEMIS_HOME=$(ARTIFACT_BASE)/opt/apache-artemis-2.6.3

build/apache-artemis-bin.tar.gz:
	curl -o build/apache-artemis-bin.tar.gz http://archive.apache.org/dist/activemq/activemq-artemis/2.6.3/apache-artemis-2.6.3-bin.tar.gz

clean_modules:
	rm -rf sasl-delegation/target
	rm -rf amqp-connector/target
	rm -rf plugin/target
	rm -rf shutdown-hook/target

clean: clean_modules

build_tar:
	mkdir -p $(ARTEMIS_HOME)/bin
	mkdir -p $(ARTEMIS_HOME)/lib
	mkdir -p $(ARTIFACT_BASE)/opt

	tar xvf plugin/target/artemis-plugin-$(VERSION)-dist.tar.gz -C $(ARTIFACT_BASE)

	# Prometheus support
	mkdir -p $(ARTIFACT_BASE)/jmx_exporter
	cp -f jmx_exporter/target/lib/jmx_prometheus_javaagent-0.1.0.jar $(ARTIFACT_BASE)/jmx_exporter/

	# Tcnative SSL
	cp -f tcnative/target/lib/netty-tcnative-boringssl-static-2.0.7.Final.jar $(ARTEMIS_HOME)/lib

	tar -czf build/artemis-image-$(VERSION).tar.gz -C build/artemis-image .

package: build_tar build/apache-artemis-bin.tar.gz

.PHONY: build_tar clean_modules
