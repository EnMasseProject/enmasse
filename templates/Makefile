include ../Makefile.common

PACKAGE_DOCS_DIR=$(TOPDIR)/documentation/html
PACKAGE_ANSIBLE_DIR=$(TOPDIR)/ansible
BUILDDIR=build
INSTALLNAME=enmasse-$(TAG)
INSTALLDIR=$(BUILDDIR)/$(INSTALLNAME)
PACKAGE_INSTALL_DIR=$(INSTALLDIR)/install
IOT_MODULES=\
	iot/auth \
	iot/core \
	iot/device-registry \
	iot/http-adapter \
	iot/mqtt-adapter \
	iot/iot-gc-s2i \
	iot/iot-operator-s2i \
	iot/qdr-configurator-s2i \
	iot/tenant \
	iot/tenant-service \

MODULES=crds address-space-controller example-roles example-plans api-server api-service grafana none-authservice prometheus service-broker cluster-service-broker standard-authservice alertmanager kube-state-metrics $(IOT_MODULES)

prepare:
	mkdir -p $(PACKAGE_INSTALL_DIR)
	mkdir -p $(PACKAGE_INSTALL_DIR)/bundles
	mkdir -p $(PACKAGE_INSTALL_DIR)/components

replace_images: prepare
	mkdir -p $(BUILDDIR)/replaced
	for i in `find $(MODULES) -type f`; do \
		D=`dirname $$i`; \
		mkdir -p $(BUILDDIR)/replaced/$$D ; \
		cp -r $$i $(BUILDDIR)/replaced/$$D/ ; \
	done
	for i in `find $(BUILDDIR)/replaced -name "*.yaml"`; do \
		cat $$i | $(IMAGE_ENV) envsubst '$${ENMASSE_VERSION},$${IMAGE_PULL_POLICY},$${ADDRESS_SPACE_CONTROLLER_IMAGE},$${STANDARD_CONTROLLER_IMAGE},$${ROUTER_IMAGE},$${NONE_AUTHSERVICE_IMAGE},$${KEYCLOAK_IMAGE},$${KEYCLOAK_CONTROLLER_IMAGE},$${KEYCLOAK_PLUGIN_IMAGE},$${TOPIC_FORWARDER_IMAGE},$${ARTEMIS_IMAGE},$${ARTEMIS_PLUGIN_IMAGE},$${ROUTER_METRICS_IMAGE},$${SUBSERV_IMAGE},$${API_SERVER_IMAGE},$${SERVICE_BROKER_IMAGE},$${AGENT_IMAGE},$${MQTT_GATEWAY_IMAGE},$${MQTT_LWT_IMAGE},$${PROMETHEUS_IMAGE},$${ALERTMANAGER_IMAGE},$${GRAFANA_IMAGE},$${KUBE_STATE_METRICS_IMAGE},$${OAUTH_PROXY_IMAGE},$${HONO_IMAGE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

component_install: replace_images
	cp -r $(BUILDDIR)/replaced/* $(PACKAGE_INSTALL_DIR)/components/
	for i in `find $(PACKAGE_INSTALL_DIR)/components -name "*.yaml"`; do \
		cat $$i | NAMESPACE=$(DEFAULT_PROJECT) envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

ansible_install: component_install
	cp -r $(PACKAGE_ANSIBLE_DIR) $(INSTALLDIR)/
	$(LN) -srf $(INSTALLDIR)/install/components $(INSTALLDIR)/ansible/playbooks/openshift/components

ENMASSE_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/enmasse
$(ENMASSE_BUNDLE): replace_images
	mkdir -p $(ENMASSE_BUNDLE)
	for i in crds address-space-controller api-server api-service; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(ENMASSE_BUNDLE)/; \
	done
	for i in `find $(ENMASSE_BUNDLE) -name "*.yaml"`; do \
		cat $$i | NAMESPACE=$(DEFAULT_PROJECT) envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

MONITORING_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/monitoring
$(MONITORING_BUNDLE): replace_images
	mkdir -p $(MONITORING_BUNDLE)
	for i in alertmanager grafana kube-state-metrics prometheus; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(MONITORING_BUNDLE)/; \
	done
	for i in `find $(MONITORING_BUNDLE) -name "*.yaml"`; do \
		cat $$i | NAMESPACE=$(DEFAULT_PROJECT) envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

IOT_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/iot
$(IOT_BUNDLE): replace_images
	mkdir -p $(IOT_BUNDLE)
	for i in $(IOT_MODULES); do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(IOT_BUNDLE)/; \
	done
	for i in `find $(IOT_BUNDLE) -name "*.yaml"`; do \
		cat $$i | NAMESPACE=$(DEFAULT_PROJECT) envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

install: ansible_install component_install $(ENMASSE_BUNDLE) $(MONITORING_BUNDLE) $(IOT_BUNDLE)
	cp -r $(PACKAGE_DOCS_DIR) $(INSTALLDIR)/docs

package: prepare install
	tar -czf build/$(INSTALLNAME).tgz -C build $(INSTALLNAME)

coverage:

.PHONY: prepare package clean $(ENMASSE_BUNDLE) $(MONITORING_BUNDLE) $(IOT_BUNDLE)
