include ../Makefile.java.mk
ARTIFACT_DIR=build/keycloak-plugin

clean_plugin:
	rm -rf sasl-plugin/target

clean: clean_plugin

build_tar:
	mkdir -p $(ARTIFACT_DIR)/providers
	mkdir -p $(ARTIFACT_DIR)/configuration
	mkdir -p $(ARTIFACT_DIR)/bin

	cp -f sasl-plugin/target/sasl-plugin-$(VERSION).jar $(ARTIFACT_DIR)/providers
	cp -f src/main/configuration/* $(ARTIFACT_DIR)/configuration
	cp -f src/main/sh/*.sh $(ARTIFACT_DIR)/bin

	tar -czf build/keycloak-plugin-$(VERSION).tar.gz -C build/keycloak-plugin .

package: build_tar

.PHONY: build_tar
