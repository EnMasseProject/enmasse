# Docker env
DOCKER_REGISTRY ?= quay.io
DOCKER_ORG      ?= enmasse
DOCKER          ?= docker
PROJECT_PREFIX  ?= enmasse
PROJECT_NAME    ?= $(shell basename $(CURDIR))
COMMIT          ?= $(shell git rev-parse HEAD)
VERSION         ?= $(shell grep "release.version" $(TOPDIR)/pom.properties| cut -d'=' -f2)
MAVEN_VERSION   ?= $(shell grep "maven.version" $(TOPDIR)/pom.properties| cut -d'=' -f2)
TAG             ?= latest

# Go settings
GOPATH          := $(abspath $(TOPDIR))/go
GOPRJ           := $(GOPATH)/src/github.com/enmasseproject/enmasse
export GOPATH

# Image settings
DOCKER_REGISTRY_PREFIX ?= $(DOCKER_REGISTRY)/
IMAGE_VERSION          ?= $(TAG)
ADDRESS_SPACE_CONTROLLER_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/address-space-controller:$(IMAGE_VERSION)
API_SERVER_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/api-server:$(IMAGE_VERSION)
STANDARD_CONTROLLER_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/standard-controller:$(IMAGE_VERSION)
ROUTER_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/router:$(IMAGE_VERSION)
BROKER_PLUGIN_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/broker-plugin:$(IMAGE_VERSION)
TOPIC_FORWARDER_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/topic-forwarder:$(IMAGE_VERSION)
AGENT_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/agent:$(IMAGE_VERSION)
MQTT_GATEWAY_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/mqtt-gateway:$(IMAGE_VERSION)
MQTT_LWT_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/mqtt-lwt:$(IMAGE_VERSION)
NONE_AUTHSERVICE_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/none-authservice:$(IMAGE_VERSION)
KEYCLOAK_IMAGE ?= jboss/keycloak-openshift:4.8.3.Final
KEYCLOAK_PLUGIN_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/keycloak-plugin:$(IMAGE_VERSION)
SERVICE_BROKER_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/service-broker:$(IMAGE_VERSION)
CONSOLE_INIT_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/console-init:$(IMAGE_VERSION)
CONSOLE_PROXY_OPENSHIFT_IMAGE ?= openshift/oauth-proxy:latest
CONSOLE_PROXY_KUBERNETES_IMAGE ?= quay.io/pusher/oauth2_proxy:latest
CONSOLE_HTTPD_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/console-httpd:$(IMAGE_VERSION)
PROMETHEUS_IMAGE ?= prom/prometheus:v2.4.3
ALERTMANAGER_IMAGE ?= prom/alertmanager:v0.15.2
GRAFANA_IMAGE ?= grafana/grafana:5.3.1
APPLICATION_MONITORING_OPERATOR_IMAGE ?= quay.io/integreatly/application-monitoring-operator:0.0.5
KUBE_STATE_METRICS_IMAGE ?= quay.io/coreos/kube-state-metrics:v1.4.0
QDROUTERD_BASE_IMAGE ?= quay.io/enmasse/qdrouterd-base:1.8.0
BROKER_IMAGE ?= quay.io/enmasse/artemis-base:2.9.0

CONTROLLER_MANAGER_IMAGE   ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/controller-manager:$(IMAGE_VERSION)

IOT_AUTH_SERVICE_IMAGE             ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-auth-service:$(IMAGE_VERSION)
IOT_DEVICE_REGISTRY_FILE_IMAGE     ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-device-registry-file:$(IMAGE_VERSION)
IOT_DEVICE_REGISTRY_INFINISPAN_IMAGE ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-device-registry-infinispan:$(IMAGE_VERSION)
IOT_GC_IMAGE                       ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-gc:$(IMAGE_VERSION)
IOT_HTTP_ADAPTER_IMAGE             ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-http-adapter:$(IMAGE_VERSION)
IOT_MQTT_ADAPTER_IMAGE             ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-mqtt-adapter:$(IMAGE_VERSION)
IOT_LORAWAN_ADAPTER_IMAGE          ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-lorawan-adapter:$(IMAGE_VERSION)
IOT_SIGFOX_ADAPTER_IMAGE           ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-sigfox-adapter:$(IMAGE_VERSION)
IOT_TENANT_SERVICE_IMAGE           ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-tenant-service:$(IMAGE_VERSION)
IOT_PROXY_CONFIGURATOR_IMAGE       ?= $(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-proxy-configurator:$(IMAGE_VERSION)


DEFAULT_PROJECT ?= enmasse-infra
ifeq ($(TAG),latest)
IMAGE_PULL_POLICY ?= Always
else
IMAGE_PULL_POLICY ?= IfNotPresent
endif

IMAGE_ENV=ADDRESS_SPACE_CONTROLLER_IMAGE=$(ADDRESS_SPACE_CONTROLLER_IMAGE) \
			API_SERVER_IMAGE=$(API_SERVER_IMAGE) \
			STANDARD_CONTROLLER_IMAGE=$(STANDARD_CONTROLLER_IMAGE) \
			ROUTER_IMAGE=$(ROUTER_IMAGE) \
			BROKER_IMAGE=$(BROKER_IMAGE) \
			BROKER_PLUGIN_IMAGE=$(BROKER_PLUGIN_IMAGE) \
			TOPIC_FORWARDER_IMAGE=$(TOPIC_FORWARDER_IMAGE) \
			SUBSERV_IMAGE=$(SUBSERV_IMAGE) \
			SERVICE_BROKER_IMAGE=$(SERVICE_BROKER_IMAGE) \
			NONE_AUTHSERVICE_IMAGE=$(NONE_AUTHSERVICE_IMAGE) \
			AGENT_IMAGE=$(AGENT_IMAGE) \
			KEYCLOAK_IMAGE=$(KEYCLOAK_IMAGE) \
			KEYCLOAK_PLUGIN_IMAGE=$(KEYCLOAK_PLUGIN_IMAGE) \
			MQTT_GATEWAY_IMAGE=$(MQTT_GATEWAY_IMAGE) \
			MQTT_LWT_IMAGE=$(MQTT_LWT_IMAGE) \
			CONSOLE_INIT_IMAGE=$(CONSOLE_INIT_IMAGE) \
			CONSOLE_PROXY_OPENSHIFT_IMAGE=$(CONSOLE_PROXY_OPENSHIFT_IMAGE) \
			CONSOLE_PROXY_KUBERNETES_IMAGE=$(CONSOLE_PROXY_KUBERNETES_IMAGE) \
			CONSOLE_HTTPD_IMAGE=$(CONSOLE_HTTPD_IMAGE) \
			PROMETHEUS_IMAGE=$(PROMETHEUS_IMAGE) \
			ALERTMANAGER_IMAGE=$(ALERTMANAGER_IMAGE) \
			GRAFANA_IMAGE=$(GRAFANA_IMAGE) \
			APPLICATION_MONITORING_OPERATOR_IMAGE=$(APPLICATION_MONITORING_OPERATOR_IMAGE) \
			KUBE_STATE_METRICS_IMAGE=$(KUBE_STATE_METRICS_IMAGE) \
			QDROUTERD_BASE_IMAGE=$(QDROUTERD_BASE_IMAGE) \
			CONTROLLER_MANAGER_IMAGE=$(CONTROLLER_MANAGER_IMAGE) \
			IOT_PROXY_CONFIGURATOR_IMAGE=$(IOT_PROXY_CONFIGURATOR_IMAGE) \
			IOT_AUTH_SERVICE_IMAGE=$(IOT_AUTH_SERVICE_IMAGE) \
			IOT_DEVICE_REGISTRY_FILE_IMAGE=$(IOT_DEVICE_REGISTRY_FILE_IMAGE) \
			IOT_GC_IMAGE=$(IOT_GC_IMAGE) \
			IOT_HTTP_ADAPTER_IMAGE=$(IOT_HTTP_ADAPTER_IMAGE) \
			IOT_MQTT_ADAPTER_IMAGE=$(IOT_MQTT_ADAPTER_IMAGE) \
			IOT_LORAWAN_ADAPTER_IMAGE=$(IOT_LORAWAN_ADAPTER_IMAGE) \
			IOT_SIGFOX_ADAPTER_IMAGE=$(IOT_SIGFOX_ADAPTER_IMAGE) \
			IOT_TENANT_SERVICE_IMAGE=$(IOT_TENANT_SERVICE_IMAGE) \
			IMAGE_PULL_POLICY=$(IMAGE_PULL_POLICY) \
			ENMASSE_VERSION=$(VERSION) \
			MAVEN_VERSION=$(MAVEN_VERSION) \
			PROJECT_PREFIX=$(PROJECT_PREFIX)
