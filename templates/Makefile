include ../Makefile.common

PACKAGE_DOCS_DIR=$(TOPDIR)/documentation/html
PACKAGE_ANSIBLE_DIR=$(TOPDIR)/ansible
BUILDDIR=build
INSTALLNAME=enmasse-$(TAG)
INSTALLDIR=$(BUILDDIR)/$(INSTALLNAME)
PACKAGE_INSTALL_DIR=$(INSTALLDIR)/install
HASHMARK = $(shell echo "\#")

IOT_MODULES=\
	iot/api \
	iot/common \
	iot/operator \

MODULES=\
	$(IOT_MODULES) \
	enmasse-operator \
	crds \
	address-space-controller \
	example-roles \
	example-plans \
	example-authservices \
	iot/examples \
	api-server \
	api-service \
	monitoring-operator \
	grafana-dashboards \
	service-broker \
	cluster-service-broker \
	kube-state-metrics \
	service-monitors \
	prometheus-rules \
	csv 

prepare:
	mkdir -p $(PACKAGE_INSTALL_DIR)
	mkdir -p $(PACKAGE_INSTALL_DIR)/bundles
	mkdir -p $(PACKAGE_INSTALL_DIR)/preview-bundles
	mkdir -p $(PACKAGE_INSTALL_DIR)/components

replace_images: prepare
	mkdir -p $(BUILDDIR)/replaced
	for i in `find $(MODULES) -type f`; do \
		D=`dirname $$i`; \
		mkdir -p $(BUILDDIR)/replaced/$$D ; \
		cp -r $$i $(BUILDDIR)/replaced/$$D/ ; \
	done
	for i in `find $(BUILDDIR)/replaced -name "*.yaml"`; do \
		cat $$i | sed -e 's,\$${ADDRESS_SPACE_CONTROLLER_IMAGE},$(ADDRESS_SPACE_CONTROLLER_IMAGE),g' \
					  -e 's,\$${MAVEN_VERSION},$(MAVEN_VERSION),g' \
					  -e 's,\$${NAMESPACE},$(DEFAULT_PROJECT),g' \
					  -e 's,\$${ENMASSE_VERSION},$(VERSION),g' \
					  -e 's,\$${IMAGE_PULL_POLICY},$(IMAGE_PULL_POLICY),g' \
					  -e 's,\$${STANDARD_CONTROLLER_IMAGE},$(STANDARD_CONTROLLER_IMAGE),g' \
					  -e 's,\$${ROUTER_IMAGE},$(ROUTER_IMAGE),g' \
					  -e 's,\$${NONE_AUTHSERVICE_IMAGE},$(NONE_AUTHSERVICE_IMAGE),g' \
					  -e 's,\$${KEYCLOAK_IMAGE},$(KEYCLOAK_IMAGE),g' \
					  -e 's,\$${KEYCLOAK_PLUGIN_IMAGE},$(KEYCLOAK_PLUGIN_IMAGE),g' \
					  -e 's,\$${TOPIC_FORWARDER_IMAGE},$(TOPIC_FORWARDER_IMAGE),g' \
					  -e 's,\$${BROKER_IMAGE},$(BROKER_IMAGE),g' \
					  -e 's,\$${BROKER_PLUGIN_IMAGE},$(BROKER_PLUGIN_IMAGE),g' \
					  -e 's,\$${SUBSERV_IMAGE},$(SUBSERV_IMAGE),g' \
					  -e 's,\$${API_SERVER_IMAGE},$(API_SERVER_IMAGE),g' \
					  -e 's,\$${SERVICE_BROKER_IMAGE},$(SERVICE_BROKER_IMAGE),g' \
					  -e 's,\$${AGENT_IMAGE},$(AGENT_IMAGE),g' \
					  -e 's,\$${MQTT_GATEWAY_IMAGE},$(MQTT_GATEWAY_IMAGE),g' \
					  -e 's,\$${MQTT_LWT_IMAGE},$(MQTT_LWT_IMAGE),g' \
					  -e 's,\$${APPLICATION_MONITORING_OPERATOR_IMAGE},$(APPLICATION_MONITORING_OPERATOR_IMAGE),g' \
					  -e 's,\$${KUBE_STATE_METRICS_IMAGE},$(KUBE_STATE_METRICS_IMAGE),g' \
					  -e 's,\$${HONO_IMAGE},$(HONO_IMAGE),g' \
					  -e 's,\$${QDROUTERD_BASE_IMAGE},$(QDROUTERD_BASE_IMAGE),g' \
					  -e 's,\$${HONO_VERSION},$(HONO_VERSION),g' \
					  -e 's,\$${IOT_GC_IMAGE},$(IOT_GC_IMAGE),g' \
					  -e 's,\$${IOT_TENANT_SERVICE_IMAGE},$(IOT_TENANT_SERVICE_IMAGE),g' \
					  -e 's,\$${IOT_AUTH_SERVICE_IMAGE},$(IOT_AUTH_SERVICE_IMAGE),g' \
					  -e 's,\$${IOT_DEVICE_REGISTRY_FILE_IMAGE},$(IOT_DEVICE_REGISTRY_FILE_IMAGE),g' \
					  -e 's,\$${IOT_HTTP_ADAPTER_IMAGE},$(IOT_HTTP_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_MQTT_ADAPTER_IMAGE},$(IOT_MQTT_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_PROXY_CONFIGURATOR_IMAGE},$(IOT_PROXY_CONFIGURATOR_IMAGE),g' \
					  -e 's,\$${CONTROLLER_MANAGER_IMAGE},$(CONTROLLER_MANAGER_IMAGE),g' \
					  -e 's,\$${CONSOLE_INIT_IMAGE},$(CONSOLE_INIT_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_OPENSHIFT_IMAGE},$(CONSOLE_PROXY_OPENSHIFT_IMAGE),g' \
					  -e 's,\$${OAUTH_PROXY_IMAGE},$(OAUTH_PROXY_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_KUBERNETES_IMAGE},$(CONSOLE_PROXY_KUBERNETES_IMAGE),g' \
					  -e 's,\$${CONSOLE_HTTPD_IMAGE},$(CONSOLE_HTTPD_IMAGE),g' > $$i.tmp; \
		mv $$i.tmp $$i; \
	done

component_install: replace_images
	cp -r $(BUILDDIR)/replaced/* $(PACKAGE_INSTALL_DIR)/components/

ansible_install: component_install
	cp -r $(PACKAGE_ANSIBLE_DIR) $(INSTALLDIR)/
	$(LN) -srf $(INSTALLDIR)/install/components $(INSTALLDIR)/ansible/playbooks/openshift/components

ENMASSE_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/enmasse
$(ENMASSE_BUNDLE): replace_images
	mkdir -p $(ENMASSE_BUNDLE)
	for i in crds address-space-controller api-server api-service enmasse-operator; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(ENMASSE_BUNDLE)/; \
	done

MONITORING_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/monitoring
$(MONITORING_BUNDLE): replace_images
	mkdir -p $(MONITORING_BUNDLE)
	for i in grafana-dashboards service-monitors prometheus-rules; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(MONITORING_BUNDLE)/; \
	done

IOT_BUNDLE=$(PACKAGE_INSTALL_DIR)/preview-bundles/iot
$(IOT_BUNDLE): replace_images
	mkdir -p $(IOT_BUNDLE)
	for i in $(IOT_MODULES); do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(IOT_BUNDLE)/; \
	done

OLM=$(PACKAGE_INSTALL_DIR)/olm/enmasse
$(OLM): replace_images
	mkdir -p $(OLM)
	for i in csv; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(OLM)/; \
	done
	for i in crds; do \
		for file in $(BUILDDIR)/replaced/$$i/*.yaml; do \
		    b=`basename $$file`; \
			newfile=$${b$(HASHMARK)010-}; \
			cp $$file $(OLM)/$${newfile}; \
		done \
	done

install: ansible_install component_install $(ENMASSE_BUNDLE) $(MONITORING_BUNDLE) $(IOT_BUNDLE) $(OLM)
	cp -r $(PACKAGE_DOCS_DIR) $(INSTALLDIR)/docs

package: prepare install
	tar -czf build/$(INSTALLNAME).tgz -C build $(INSTALLNAME)

coverage:

.PHONY: prepare package clean $(ENMASSE_BUNDLE) $(MONITORING_BUNDLE) $(IOT_BUNDLE) $(OLM)
