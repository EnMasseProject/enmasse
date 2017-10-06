include ../Makefile.java.mk
ARTIFACT_BASE=build/artemis-image
ARTEMIS_HOME=$(ARTIFACT_BASE)/opt/apache-artemis-2.3.0

build/apache-artemis-bin.tar.gz:
	curl -o build/apache-artemis-bin.tar.gz http://archive.apache.org/dist/activemq/activemq-artemis/2.3.0/apache-artemis-2.3.0-bin.tar.gz

build_tar:
	mkdir -p $(ARTEMIS_HOME)/bin
	mkdir -p $(ARTEMIS_HOME)/lib

	cp -f activemq-artemis/artemis-protocols/artemis-amqp-protocol/target/artemis-amqp-protocol-2.3.0.jar $(ARTEMIS_HOME)/lib
	cp -f amqp-connector/target/amqp-connector.jar $(ARTEMIS_HOME)/lib
	cp -f sasl-delegation/target/sasl-delegation.jar $(ARTEMIS_HOME)/lib
	cp -r config_templates $(ARTIFACT_BASE)/
	cp -r utils/* $(ARTEMIS_HOME)/bin
	mkdir -p $(ARTIFACT_BASE)/shutdown-hook
	cp -r shutdown-hook/target/shutdown-hook.jar $(ARTIFACT_BASE)/shutdown-hook/
	cp -r shutdown-hook/src/main/sh/shutdown-hook.sh $(ARTIFACT_BASE)/shutdown-hook/
	tar -czf build/artemis-image.tar -C build/artemis-image .

package: build_tar build/apache-artemis-bin.tar.gz

.PHONY: build_tar
