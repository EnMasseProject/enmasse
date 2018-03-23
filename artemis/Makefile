include ../Makefile.java.mk
ARTIFACT_BASE=build/artemis-image
ARTEMIS_HOME=$(ARTIFACT_BASE)/opt/apache-artemis-2.5.0

build/apache-artemis-bin.tar.gz:
	curl -o build/apache-artemis-bin.tar.gz http://archive.apache.org/dist/activemq/activemq-artemis/2.5.0/apache-artemis-2.5.0-bin.tar.gz

clean_modules:
	rm -rf sasl-delegation/target
	rm -rf amqp-connector/target

clean: clean_modules

build_tar:
	mkdir -p $(ARTEMIS_HOME)/bin
	mkdir -p $(ARTEMIS_HOME)/lib
	mkdir -p $(ARTIFACT_BASE)/usr/local
	mkdir -p $(ARTIFACT_BASE)/opt

	cp -f amqp-connector/target/amqp-connector-$(VERSION).jar $(ARTEMIS_HOME)/lib
	cp -f sasl-delegation/target/sasl-delegation-$(VERSION).jar $(ARTEMIS_HOME)/lib
	cp -r config_templates $(ARTIFACT_BASE)/
	cp -r utils/bin $(ARTEMIS_HOME)
	cp -r utils/dynamic-resources $(ARTIFACT_BASE)/usr/local
	cp -r utils/run-java $(ARTIFACT_BASE)/opt
	mkdir -p $(ARTIFACT_BASE)/shutdown-hook
	cp -r shutdown-hook/target/shutdown-hook-$(VERSION).jar $(ARTIFACT_BASE)/shutdown-hook/shutdown-hook.jar
	cp -r shutdown-hook/src/main/sh/shutdown-hook.sh $(ARTIFACT_BASE)/shutdown-hook/

	mkdir -p $(ARTIFACT_BASE)/jmx_exporter
	cp -f jmx_exporter/target/lib/jmx_prometheus_javaagent-0.1.0.jar $(ARTIFACT_BASE)/jmx_exporter/

	tar -czf build/artemis-image-$(VERSION).tar.gz -C build/artemis-image .

package: build_tar build/apache-artemis-bin.tar.gz

.PHONY: build_tar clean_modules
