include ../Makefile.java.mk
ARTIFACT_BASE=build/broker-plugin-image
ARTEMIS_PLUGIN_HOME=$(ARTIFACT_BASE)/opt/broker-plugin

clean_modules:
	rm -rf sasl-delegation/target
	rm -rf amqp-connector/target
	rm -rf broker-cli/target

clean: clean_modules

build_tar:
	mkdir -p $(ARTIFACT_BASE)/usr/local
	mkdir -p $(ARTIFACT_BASE)/opt

	tar xvf plugin/target/plugin-$(MAVEN_VERSION)-dist.tar.gz -C $(ARTIFACT_BASE)

	tar -czf build/broker-plugin-$(MAVEN_VERSION)-dist.tar.gz -C $(ARTIFACT_BASE) .

package: build_tar

.PHONY: build_tar clean_modules
