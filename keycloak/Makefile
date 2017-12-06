include ../Makefile.java.mk
ARTIFACT_DIR=build/keycloak

clean_plugin:
	rm -rf sasl-plugin/target

clean: clean_plugin

build_tar:
	mkdir -p $(ARTIFACT_DIR)/providers
	mkdir -p $(ARTIFACT_DIR)/bin

	cp -f sasl-plugin/target/sasl-plugin-$(VERSION).jar $(ARTIFACT_DIR)/providers
	cp -f src/main/sh/*.sh $(ARTIFACT_DIR)/bin
	cp -f src/main/xsl/*.xsl $(ARTIFACT_DIR)

	tar -czf build/keycloak-$(VERSION).tar.gz -C build/keycloak .

package: build_tar

.PHONY: build_tar
