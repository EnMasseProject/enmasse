# WARNING: this file has the be included last, otherwise the whole
# build silently fails since VERSION (etc) and TOPDIR are defined
# as a recursive variable and based on the location on the "last"
# included makefile, which might change over time, processing other
# makefiles.
include ../Makefile.common

BUILDDIR=build
INSTALLNAME=enmasse-$(TAG)
INSTALLDIR=$(BUILDDIR)/$(INSTALLNAME)
PACKAGE_INSTALL_DIR=$(INSTALLDIR)/install
HASHMARK = $(shell echo "\#")

IOT_MODULES=\
	iot/common \
	iot/operator \

MODULES=\
	$(IOT_MODULES) \
	enmasse-operator \
	helm \
	crds \
	console-server \
	example-roles \
	example-console \
	example-olm \
	iot/examples \
	monitoring-operator \
	monitoring-deployment \
	kube-state-metrics

prepare:
	mkdir -p $(PACKAGE_INSTALL_DIR)
	mkdir -p $(PACKAGE_INSTALL_DIR)/bundles
	mkdir -p $(PACKAGE_INSTALL_DIR)/components

replace_images: prepare
	rm -rf $(BUILDDIR)/replaced; mkdir -p $(BUILDDIR)/replaced
	for i in `find $(MODULES) -type f`; do \
		D=`dirname $$i`; \
		mkdir -p $(BUILDDIR)/replaced/$$D ; \
		cp -r $$i $(BUILDDIR)/replaced/$$D/ ; \
	done
	for i in `find $(BUILDDIR)/replaced/crds -type f`; do \
		F=`basename $$i`; \
		mv $$i $(BUILDDIR)/replaced/crds/010-$$F ; \
	done
	for i in `find $(BUILDDIR)/replaced -name "*.yaml"`; do \
		cat $$i | sed -e 's,\$${MAVEN_VERSION},$(MAVEN_VERSION),g' \
					  -e 's,\$${REVISION},$(REVISION),g' \
					  -e 's,\$${NAMESPACE},$(DEFAULT_PROJECT),g' \
					  -e 's,\$${VERSION},$(VERSION),g' \
					  -e 's,\$${CONSOLE_LINK_SECTION_NAME},$(CONSOLE_LINK_SECTION_NAME),g' \
					  -e 's,\$${CONSOLE_LINK_NAME},$(CONSOLE_LINK_NAME),g' \
					  -e 's|\$${CONSOLE_LINK_IMAGE_URL}|$(CONSOLE_LINK_IMAGE_URL)|g' \
					  -e 's,\$${OLM_VERSION},$(OLM_VERSION),g' \
					  -e 's,\$${OLM_PACKAGE_CHANNEL},$(OLM_PACKAGE_CHANNEL),g' \
					  -e 's,\$${APP_BUNDLE_PREFIX},$(APP_BUNDLE_PREFIX),g' \
					  -e 's,\$${IMAGE_PULL_POLICY},$(IMAGE_PULL_POLICY),g' \
					  -e 's,\$${ROUTER_IMAGE},$(ROUTER_IMAGE),g' \
					  -e 's,\$${BROKER_IMAGE},$(BROKER_IMAGE),g' \
					  -e 's,\$${BROKER_PLUGIN_IMAGE},$(BROKER_PLUGIN_IMAGE),g' \
					  -e 's,\$${APPLICATION_MONITORING_OPERATOR_IMAGE},$(APPLICATION_MONITORING_OPERATOR_IMAGE),g' \
					  -e 's,\$${KUBE_STATE_METRICS_IMAGE},$(KUBE_STATE_METRICS_IMAGE),g' \
					  -e 's,\$${HONO_IMAGE},$(HONO_IMAGE),g' \
					  -e 's,\$${IOT_TENANT_CLEANER_IMAGE},$(IOT_TENANT_CLEANER_IMAGE),g' \
					  -e 's,\$${IOT_TENANT_SERVICE_IMAGE},$(IOT_TENANT_SERVICE_IMAGE),g' \
					  -e 's,\$${IOT_AUTH_SERVICE_IMAGE},$(IOT_AUTH_SERVICE_IMAGE),g' \
					  -e 's,\$${IOT_DEVICE_CONNECTION_INFINISPAN_IMAGE},$(IOT_DEVICE_CONNECTION_INFINISPAN_IMAGE),g' \
					  -e 's,\$${IOT_DEVICE_CONNECTION_JDBC_IMAGE},$(IOT_DEVICE_CONNECTION_JDBC_IMAGE),g' \
					  -e 's,\$${IOT_DEVICE_REGISTRY_INFINISPAN_IMAGE},$(IOT_DEVICE_REGISTRY_INFINISPAN_IMAGE),g' \
					  -e 's,\$${IOT_DEVICE_REGISTRY_JDBC_IMAGE},$(IOT_DEVICE_REGISTRY_JDBC_IMAGE),g' \
					  -e 's,\$${IOT_AMQP_ADAPTER_IMAGE},$(IOT_AMQP_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_HTTP_ADAPTER_IMAGE},$(IOT_HTTP_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_MQTT_ADAPTER_IMAGE},$(IOT_MQTT_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_LORAWAN_ADAPTER_IMAGE},$(IOT_LORAWAN_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_SIGFOX_ADAPTER_IMAGE},$(IOT_SIGFOX_ADAPTER_IMAGE),g' \
					  -e 's,\$${IOT_PROXY_CONFIGURATOR_IMAGE},$(IOT_PROXY_CONFIGURATOR_IMAGE),g' \
					  -e 's,\$${CONTROLLER_MANAGER_IMAGE},$(CONTROLLER_MANAGER_IMAGE),g' \
					  -e 's,\$${OLM_MANIFEST_IMAGE},$(OLM_MANIFEST_IMAGE),g' \
					  -e 's,\$${CONSOLE_SERVER_IMAGE},$(CONSOLE_SERVER_IMAGE),g' \
					  -e 's,\$${CONSOLE_INIT_IMAGE},$(CONSOLE_INIT_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_OPENSHIFT_IMAGE},$(CONSOLE_PROXY_OPENSHIFT_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_KUBERNETES_IMAGE},$(CONSOLE_PROXY_KUBERNETES_IMAGE),g' \
					  -e 's,\$${ACCESS_CONTROL_SERVER_IMAGE},$(ACCESS_CONTROL_SERVER_IMAGE),g' \
					  -e 's,\$${APP_PREFIX},$(PROJECT_PREFIX),g' \
					> $$i.tmp; \
		mv $$i.tmp $$i; \
	done

component_install: replace_images
	cp -r $(BUILDDIR)/replaced/* $(PACKAGE_INSTALL_DIR)/components/

HELM_CHART=$(PACKAGE_INSTALL_DIR)/helm
$(HELM_CHART): replace_images
	mkdir -p $(HELM_CHART)/templates
	cp $(BUILDDIR)/replaced/helm/Chart.yaml $(HELM_CHART)/
	cp -r $(BUILDDIR)/replaced/crds $(HELM_CHART)/
	cp -r ../LICENSE $(HELM_CHART)/
	for i in enmasse-operator console-server $(IOT_MODULES); do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(HELM_CHART)/templates; \
	done
	sed -i -e 's/$(DEFAULT_PROJECT)/{{ .Release.Namespace }}/g' $(HELM_CHART)/templates/*

ENMASSE_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/enmasse
$(ENMASSE_BUNDLE): replace_images
	mkdir -p $(ENMASSE_BUNDLE)
	for i in crds enmasse-operator console-server $(IOT_MODULES); do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(ENMASSE_BUNDLE)/; \
	done

install: component_install $(ENMASSE_BUNDLE) $(HELM_CHART)
	@echo "Preparing installation bundle"

package: prepare install
	tar -czf build/$(INSTALLNAME).tgz -C build $(INSTALLNAME)

coverage:

.PHONY: prepare package clean $(ENMASSE_BUNDLE) $(HELM_CHART)
