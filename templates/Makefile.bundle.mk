## WARNING: this file has the be included last, otherwise the whole
## build silently fails since VERSION (etc) and TOPDIR are defined
## as a recursive variable and based on the location on the "last"
## included makefile, which might change over time, processing other
## makefiles.
include ../Makefile.common

PACKAGE_ANSIBLE_DIR=$(TOPDIR)/ansible

ifeq ($(BUILDDIR),)
  $(error BUILDDIR is not set)
endif
ifeq ($(INSTALLNAME),)
  $(error INSTALLNAME is not set)
endif
ifeq ($(CRDVER),)
  $(error CRDVER is not set)
endif

#BUILDDIR=build
INSTALLDIR=$(BUILDDIR)/$(INSTALLNAME)
PACKAGE_INSTALL_DIR=$(INSTALLDIR)/install
HASHMARK = $(shell echo "\#")

MODULES=\
	enmasse-operator \
	crds/$(CRDVER) \
	address-space-controller \
	console-server \
	example-roles \
	example-plans \
	example-console \
	example-authservices \
	example-olm \
	monitoring-operator \
	monitoring-deployment \
	service-broker \
	cluster-service-broker \
	kube-state-metrics

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
	for i in `find $(BUILDDIR)/replaced/crds -type f  -not -name "010-*" `; do \
		F=`basename $$i`; \
		mv $$i $(BUILDDIR)/replaced/crds/010-$$F ; \
	done
	for i in `find $(BUILDDIR)/replaced -name "*.yaml"`; do \
		cat $$i | sed -e 's,\$${ADDRESS_SPACE_CONTROLLER_IMAGE},$(ADDRESS_SPACE_CONTROLLER_IMAGE),g' \
					  -e 's,\$${MAVEN_VERSION},$(MAVEN_VERSION),g' \
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
					  -e 's,\$${APPLICATION_MONITORING_OPERATOR_IMAGE},$(APPLICATION_MONITORING_OPERATOR_IMAGE),g' \
					  -e 's,\$${KUBE_STATE_METRICS_IMAGE},$(KUBE_STATE_METRICS_IMAGE),g' \
					  -e 's,\$${HONO_IMAGE},$(HONO_IMAGE),g' \
					  -e 's,\$${CONTROLLER_MANAGER_IMAGE},$(CONTROLLER_MANAGER_IMAGE),g' \
					  -e 's,\$${OLM_MANIFEST_IMAGE},$(OLM_MANIFEST_IMAGE),g' \
					  -e 's,\$${OLM_INDEX_IMAGE},$(OLM_INDEX_IMAGE),g' \
					  -e 's,\$${CONSOLE_SERVER_IMAGE},$(CONSOLE_SERVER_IMAGE),g' \
					  -e 's,\$${CONSOLE_INIT_IMAGE},$(CONSOLE_INIT_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_OPENSHIFT_IMAGE},$(CONSOLE_PROXY_OPENSHIFT_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_OPENSHIFT3_IMAGE},$(CONSOLE_PROXY_OPENSHIFT3_IMAGE),g' \
					  -e 's,\$${CONSOLE_PROXY_KUBERNETES_IMAGE},$(CONSOLE_PROXY_KUBERNETES_IMAGE),g' \
					  -e 's,\$${APP_PREFIX},$(PROJECT_PREFIX),g' \
					> $$i.tmp; \
		mv $$i.tmp $$i; \
	done

component_install: replace_images
	cp -r $(BUILDDIR)/replaced/* $(PACKAGE_INSTALL_DIR)/components/

ansible_install: component_install
	cp -r $(PACKAGE_ANSIBLE_DIR) $(INSTALLDIR)/
	$(LN) -srfT $(INSTALLDIR)/install/components $(INSTALLDIR)/ansible/playbooks/openshift/components

scripts_install:
	cp -r scripts $(INSTALLDIR)/

ENMASSE_BUNDLE=$(PACKAGE_INSTALL_DIR)/bundles/enmasse
$(ENMASSE_BUNDLE): replace_images
	mkdir -p $(ENMASSE_BUNDLE)
	for i in crds address-space-controller enmasse-operator console-server; do \
		cp $(BUILDDIR)/replaced/$$i/*.yaml $(ENMASSE_BUNDLE)/; \
	done

install: ansible_install scripts_install component_install $(ENMASSE_BUNDLE)
	@echo "Preparing installation bundle"

package: prepare install
	tar -czf $(BUILDDIR)/$(INSTALLNAME).tgz -C $(BUILDDIR) $(INSTALLNAME)

coverage:

.PHONY: prepare package clean $(ENMASSE_BUNDLE)
