include ../Makefile.common

ADDRESS_CONTROLLER_IMAGE ?= "address-controller"
STANDARD_CONTROLLER_IMAGE ?= "standard-controller"
ROUTER_IMAGE ?= "router"
ARTEMIS_IMAGE ?= "artemis"
TOPIC_FORWARDER_IMAGE ?= "topic-forwarder"
ROUTER_METRICS_IMAGE ?= "router-metrics"
CONFIGSERV_IMAGE ?= "configserv"
QUEUE_SCHEDULER_IMAGE ?= "queue-scheduler"
RAGENT_IMAGE ?= "ragent"
SUBSERV_IMAGE ?= "subserv"
AGENT_IMAGE ?= "agent"
MQTT_GATEWAY_IMAGE ?= "mqtt-gateway"
MQTT_LWT_IMAGE ?= "mqtt-lwt"
NONE_AUTHSERVICE_IMAGE ?= "none-authservice"
KEYCLOAK_IMAGE ?= "jboss/keycloak-openshift:3.4.3.Final"
KEYCLOAK_PLUGIN_IMAGE ?= "keycloak-plugin"
KEYCLOAK_CONTROLLER_IMAGE ?= "keycloak-controller"

KEYCLOAK_HOME ?= "/opt/jboss/keycloak"
ADD_KEYCLOAK_USER ?= "false"
NODE_CMD="node"

SRCS=$(wildcard *.jsonnet)
OBJS=$(patsubst %.jsonnet,%.json,$(SRCS))
INSTALLDIR=enmasse-$(TAG)

%.json: %.jsonnet
	jsonnet/jsonnet -V VERSION=$(COMMIT) \
		-V REGISTRY=$(DOCKER_REGISTRY) \
		-V PROJECT=$(DOCKER_ORG) \
		-V ADDRESS_CONTROLLER_IMAGE=$(ADDRESS_CONTROLLER_IMAGE) \
		-V STANDARD_CONTROLLER_IMAGE=$(STANDARD_CONTROLLER_IMAGE) \
		-V ROUTER_IMAGE=$(ROUTER_IMAGE) \
		-V ARTEMIS_IMAGE=$(ARTEMIS_IMAGE) \
		-V TOPIC_FORWARDER_IMAGE=$(TOPIC_FORWARDER_IMAGE) \
		-V ROUTER_METRICS_IMAGE=$(ROUTER_METRICS_IMAGE) \
		-V CONFIGSERV_IMAGE=$(CONFIGSERV_IMAGE) \
		-V QUEUE_SCHEDULER_IMAGE=$(QUEUE_SCHEDULER_IMAGE) \
		-V RAGENT_IMAGE=$(RAGENT_IMAGE) \
		-V SUBSERV_IMAGE=$(SUBSERV_IMAGE) \
		-V AGENT_IMAGE=$(AGENT_IMAGE) \
		-V MQTT_GATEWAY_IMAGE=$(MQTT_GATEWAY_IMAGE) \
		-V MQTT_LWT_IMAGE=$(MQTT_LWT_IMAGE) \
		-V NONE_AUTHSERVICE_IMAGE=$(NONE_AUTHSERVICE_IMAGE) \
		-V KEYCLOAK_IMAGE=$(KEYCLOAK_IMAGE) \
		-V KEYCLOAK_PLUGIN_IMAGE=$(KEYCLOAK_PLUGIN_IMAGE) \
		-V KEYCLOAK_CONTROLLER_IMAGE=$(KEYCLOAK_CONTROLLER_IMAGE) \
		-V KEYCLOAK_HOME=$(KEYCLOAK_HOME) \
		-V ADD_KEYCLOAK_USER=$(ADD_KEYCLOAK_USER) \
		-V NODE_CMD=$(NODE_CMD) \
		-m build $<

yaml:
	for d in kubernetes openshift; do for i in `find build/$$d -name "*.json"`; do b=`dirname $$i`; o="install/$${b#build/}"; mkdir -p $$o; ./scripts/convertyaml.py $$i $$o; done; done

prepare:
	if [ ! -f jsonnet ]; then $(MAKE) -C jsonnet; fi
	mkdir -p build/kubernetes/addons
	mkdir -p build/openshift/addons
	cp include/*.json build
	cp -f include/resource-definitions.yaml install/kubernetes/addons/
	cp -f include/resource-definitions.yaml install/openshift/addons/
	cp -f include/*-plans.yaml install/kubernetes/addons/
	cp -f include/*-plans.yaml install/openshift/addons/
	cp -f include/address.yaml include/address-space.yaml install/openshift/

package: prepare $(OBJS) yaml
	mkdir -p build/$(INSTALLDIR)                                                                                                                                               
	cp -r install/* build/$(INSTALLDIR)                                                                                                                                        
	tar -czf build/$(INSTALLDIR).tgz -C build $(INSTALLDIR)

coverage:

.PHONY: yaml prepare package clean
