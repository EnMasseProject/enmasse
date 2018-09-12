include ../Makefile.common

DOCKER_REGISTRY_PREFIX ?= $(DOCKER_REGISTRY)/

ADDRESS_SPACE_CONTROLLER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/address-space-controller:$(COMMIT)"
API_SERVER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/api-server:$(COMMIT)"
STANDARD_CONTROLLER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/standard-controller:$(COMMIT)"
ROUTER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/router:$(COMMIT)"
ARTEMIS_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/artemis:$(COMMIT)"
TOPIC_FORWARDER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/topic-forwarder:$(COMMIT)"
ROUTER_METRICS_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/router-metrics:$(COMMIT)"
AGENT_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/agent:$(COMMIT)"
MQTT_GATEWAY_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/mqtt-gateway:$(COMMIT)"
MQTT_LWT_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/mqtt-lwt:$(COMMIT)"
NONE_AUTHSERVICE_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/none-authservice:$(COMMIT)"
KEYCLOAK_IMAGE ?= "jboss/keycloak-openshift:3.4.3.Final"
KEYCLOAK_PLUGIN_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/keycloak-plugin:$(COMMIT)"
KEYCLOAK_CONTROLLER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/keycloak-controller:$(COMMIT)"
SERVICE_BROKER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/service-broker:$(COMMIT)"

DOCSDIR=$(TOPDIR)/documentation/html
ANSIBLEDIR=$(TOPDIR)/ansible
BUILDDIR=build
INSTALLNAME=enmasse-$(TAG)
INSTALLDIR=$(BUILDDIR)/$(INSTALLNAME)
EXAMPLESDIR=$(INSTALLDIR)/examples
MODULES=address-space-controller extra api-server api-service grafana none-authservice prometheus service-broker cluster-service-broker standard-authservice tenant

prepare:
	mkdir -p $(EXAMPLESDIR)
	mkdir -p $(EXAMPLESDIR)/templates
	mkdir -p $(EXAMPLESDIR)/bundles
	mkdir -p $(EXAMPLESDIR)/install

replace_images: prepare
	mkdir -p $(BUILDDIR)/replaced
	cp -r $(MODULES) $(BUILDDIR)/replaced
	for i in `find $(BUILDDIR)/replaced -name "*.yaml"`; do \
		cat $$i | ADDRESS_SPACE_CONTROLLER_IMAGE=$(ADDRESS_SPACE_CONTROLLER_IMAGE) \
			API_SERVER_IMAGE=$(API_SERVER_IMAGE) \
			STANDARD_CONTROLLER_IMAGE=$(STANDARD_CONTROLLER_IMAGE) \
			ROUTER_IMAGE=$(ROUTER_IMAGE) \
			ARTEMIS_IMAGE=$(ARTEMIS_IMAGE) \
			TOPIC_FORWARDER_IMAGE=$(TOPIC_FORWARDER_IMAGE) \
			ROUTER_METRICS_IMAGE=$(ROUTER_METRICS_IMAGE) \
			SUBSERV_IMAGE=$(SUBSERV_IMAGE) \
			SERVICE_BROKER_IMAGE=$(SERVICE_BROKER_IMAGE) \
			NONE_AUTHSERVICE_IMAGE=$(NONE_AUTHSERVICE_IMAGE) \
			AGENT_IMAGE=$(AGENT_IMAGE) \
			KEYCLOAK_IMAGE=$(KEYCLOAK_IMAGE) \
			KEYCLOAK_PLUGIN_IMAGE=$(KEYCLOAK_PLUGIN_IMAGE) \
			KEYCLOAK_CONTROLLER_IMAGE=$(KEYCLOAK_CONTROLLER_IMAGE) \
			MQTT_GATEWAY_IMAGE=$(MQTT_GATEWAY_IMAGE) \
			MQTT_LWT_IMAGE=$(MQTT_LWT_IMAGE) \
			envsubst '$${ADDRESS_SPACE_CONTROLLER_IMAGE},$${STANDARD_CONTROLLER_IMAGE},$${ROUTER_IMAGE},$${NONE_AUTHSERVICE_IMAGE},$${KEYCLOAK_IMAGE},$${KEYCLOAK_CONTROLLER_IMAGE},$${KEYCLOAK_PLUGIN_IMAGE},$${TOPIC_FORWARDER_IMAGE},$${ARTEMIS_IMAGE},$${ROUTER_METRICS_IMAGE},$${SUBSERV_IMAGE},$${API_SERVER_IMAGE},$${SERVICE_BROKER_IMAGE},$${AGENT_IMAGE},$${MQTT_GATEWAY_IMAGE},$${MQTT_LWT_IMAGE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

component_install: replace_images
	cp -r $(BUILDDIR)/replaced/* $(EXAMPLESDIR)/install/
	for i in `find $(EXAMPLESDIR)/install -name "*.yaml"`; do \
		cat $$i | NAMESPACE=myproject envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

ansible_install: replace_images
	cp -r $(ANSIBLEDIR) $(INSTALLDIR)/
	mkdir -p $(INSTALLDIR)/ansible/playbooks/openshift/install/
	cp -r $(BUILDDIR)/replaced/* $(INSTALLDIR)/ansible/playbooks/openshift/install/
	for i in `find $(INSTALLDIR)/ansible/playbooks/openshift/ -name "*.yaml"`; do \
		cat $$i | NAMESPACE=ANSIBLE_NAMESPACE envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE=$(EXAMPLESDIR)/templates/enmasse-with-standard-authservice.yaml
$(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE): replace_images
	echo 'apiVersion: v1' > $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo 'kind: Template' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo 'metadata:' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo '  name: enmasse-with-standard-authservice' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo 'parameters:' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo '- name: NAMESPACE' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo '  description: Namespace where EnMasse should be deployed' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo '  required: true' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	echo 'objects:' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE)
	for mod in address-space-controller api-server api-service standard-authservice tenant; do \
		for i in `find $(BUILDDIR)/replaced/$${mod} -name "*.yaml"`; do \
			echo "Mod $${mod} file $${i}"; \
			cat $$i | sed 's/^/  /' | sed '1s/^  /- /' >> $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE); \
		done; \
	done

ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE=$(EXAMPLESDIR)/templates/enmasse-with-none-authservice.yaml
$(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE): replace_images
	echo 'apiVersion: v1' > $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo 'kind: Template' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo 'metadata:' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo '  name: enmasse-with-none-authservice' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo 'parameters:' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo '- name: NAMESPACE' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo '  description: Namespace where EnMasse should be deployed' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo '  required: true' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	echo 'objects:' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	for mod in address-space-controller api-server api-service none-authservice tenant; do \
		for i in `find $(BUILDDIR)/replaced/$${mod} -name "*.yaml"`; do \
			cat $$i | sed 's/^/  /' | sed '1s/^  /- /' >> $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE); \
		done; \
	done

ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE=$(EXAMPLESDIR)/bundles/enmasse-with-none-authservice
$(ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE): replace_images
	mkdir -p $(ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE)
	for i in address-space-controller extra api-server api-service none-authservice tenant; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE)/; \
	done
	for i in `find $(ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE) -name "*.yaml"`; do \
		cat $$i | NAMESPACE=myproject envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE=$(EXAMPLESDIR)/bundles/enmasse-with-standard-authservice
$(ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE): replace_images
	mkdir -p $(ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE)
	for i in address-space-controller extra api-server api-service standard-authservice tenant; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE)/; \
	done
	for i in `find $(ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE) -name "*.yaml"`; do \
		cat $$i | NAMESPACE=myproject envsubst '$${NAMESPACE}' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

install: ansible_install component_install $(ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE) $(ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE) $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE) $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
	cp -r $(DOCSDIR) $(INSTALLDIR)/docs

package: prepare install
	tar -czf build/$(INSTALLNAME).tgz -C build $(INSTALLNAME)

coverage:

.PHONY: prepare package clean $(ENMASSE_WITH_STANDARD_AUTHSERVICE_BUNDLE) $(ENMASSE_WITH_NONE_AUTHSERVICE_BUNDLE) $(ENMASSE_WITH_STANDARD_AUTHSERVICE_TEMPLATE) $(ENMASSE_WITH_NONE_AUTHSERVICE_TEMPLATE)
