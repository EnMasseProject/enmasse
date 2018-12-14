include ../Makefile.java.mk
ARTIFACT_BASE=build/broker-plugin-image
ARTEMIS_PLUGIN_HOME=$(ARTIFACT_BASE)/opt/broker-plugin

clean_modules:
	rm -rf sasl-delegation/target
	rm -rf amqp-connector/target

clean: clean_modules

build_tar:
	mkdir -p $(ARTIFACT_BASE)/usr/local
	mkdir -p $(ARTIFACT_BASE)/opt

	tar xvf plugin/target/plugin-$(VERSION)-dist.tar.gz -C $(ARTIFACT_BASE)
	
	# Prometheus support
	mkdir -p $(ARTEMIS_PLUGIN_HOME)/jmx_exporter
	cp -f jmx_exporter/target/lib/jmx_prometheus_javaagent-0.1.0.jar $(ARTEMIS_PLUGIN_HOME)/jmx_exporter/

	# Tcnative SSL
	cp -f tcnative/target/lib/netty-tcnative-boringssl-static-2.0.7.Final.jar $(ARTEMIS_PLUGIN_HOME)/lib

	tar -czf build/broker-plugin-$(VERSION)-dist.tar.gz -C $(ARTIFACT_BASE) .

package: build_tar

.PHONY: build_tar clean_modules
