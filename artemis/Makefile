include ../Makefile.java.mk
ARTIFACT_BASE=build/artemis-image
ARTEMIS_HOME=$(ARTIFACT_BASE)/opt/apache-artemis

build/apache-artemis-bin.tar.gz:
	curl -o build/apache-artemis-bin.tar.gz http://archive.apache.org/dist/activemq/activemq-artemis/2.6.3/apache-artemis-2.6.3-bin.tar.gz

clean_modules:
	rm -rf sasl-delegation/target
	rm -rf amqp-connector/target
	rm -rf plugin/target

clean: clean_modules

build_tar:
	mkdir -p $(ARTEMIS_HOME)/bin
	mkdir -p $(ARTEMIS_HOME)/lib
	mkdir -p $(ARTIFACT_BASE)/opt

	cp -r utils/bin $(ARTEMIS_HOME)

	tar -czf build/artemis-image-$(MAVEN_VERSION).tar.gz -C build/artemis-image .

package: build_tar build/apache-artemis-bin.tar.gz

.PHONY: build_tar clean_modules
