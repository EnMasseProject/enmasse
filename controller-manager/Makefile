CMD=controller-manager
include ../Makefile.go.mk

build_deps: build/operatorImageMap.yaml

run: build/operatorImageMap.yaml 
	CONTROLLER_DISABLE_ALL=true CONTROLLER_ENABLE_MESSAGING_INFRASTRUCTURE=true OPERATOR_NAME=enmasse-operator NAMESPACE=enmasse-infra ENMASSE_IMAGE_MAP_FILE=./build/operatorImageMap.yaml ./build/controller-manager

build/operatorImageMap.yaml:
	mkdir -p build
	echo "" > "$@"

	echo "address-space-controller: $(ADDRESS_SPACE_CONTROLLER_IMAGE)" >> "$@"
	echo "controller-manager: $(CONTROLLER_MANAGER_IMAGE)" >> "$@"
	echo "iot-auth-service: $(IOT_AUTH_SERVICE_IMAGE)" >> "$@"
	echo "iot-device-connection-infinispan: $(IOT_DEVICE_CONNECTION_INFINISPAN_IMAGE)" >> "$@"
	echo "iot-device-connection-jdbc: $(IOT_DEVICE_CONNECTION_JDBC_IMAGE)" >> "$@"
	echo "iot-device-registry-infinispan: $(IOT_DEVICE_REGISTRY_INFINISPAN_IMAGE)" >> "$@"
	echo "iot-device-registry-jdbc: $(IOT_DEVICE_REGISTRY_JDBC_IMAGE)" >> "$@"
	echo "iot-amqp-adapter: $(IOT_AMQP_ADAPTER_IMAGE)" >> "$@"
	echo "iot-http-adapter: $(IOT_HTTP_ADAPTER_IMAGE)" >> "$@"
	echo "iot-mqtt-adapter: $(IOT_MQTT_ADAPTER_IMAGE)" >> "$@"
	echo "iot-lorawan-adapter: $(IOT_LORAWAN_ADAPTER_IMAGE)" >> "$@"
	echo "iot-sigfox-adapter: $(IOT_SIGFOX_ADAPTER_IMAGE)" >> "$@"
	echo "iot-tenant-cleaner: $(IOT_TENANT_CLEANER_IMAGE)" >> "$@"
	echo "iot-tenant-service: $(IOT_TENANT_SERVICE_IMAGE)" >> "$@"
	echo "iot-proxy-configurator: $(IOT_PROXY_CONFIGURATOR_IMAGE)" >> "$@"
	echo "router: $(ROUTER_IMAGE)" >> "$@"
	echo "none-authservice: $(NONE_AUTHSERVICE_IMAGE)" >> "$@"
	echo "keycloak: $(KEYCLOAK_IMAGE)" >> "$@"
	echo "keycloak-plugin: $(KEYCLOAK_PLUGIN_IMAGE)" >> "$@"
	echo "access-control-server: $(+ACCESS_CONTROL_SERVER_IMAGE)" >> "$@"
	echo "console-init: $(CONSOLE_INIT_IMAGE)" >> "$@"
	echo "console-server: $(CONSOLE_SERVER_IMAGE)" >> "$@"
	echo "console-proxy-openshift: $(CONSOLE_PROXY_OPENSHIFT_IMAGE)" >> "$@"
	echo "console-proxy-kubernetes: $(CONSOLE_PROXY_KUBERNETES_IMAGE)" >> "$@"
	echo "standard-controller: $(STANDARD_CONTROLLER_IMAGE)" >> "$@"
	echo "agent: $(AGENT_IMAGE)" >> "$@"
	echo "broker: $(BROKER_IMAGE)" >> "$@"
	echo "broker-plugin: $(BROKER_PLUGIN_IMAGE)" >> "$@"
	echo "topic-forwarder: $(TOPIC_FORWARDER_IMAGE)" >> "$@"
	echo "mqtt-gateway: $(MQTT_GATEWAY_IMAGE)" >> "$@"
	echo "mqtt-lwt: $(MQTT_LWT_IMAGE)" >> "$@"

.PHONY: build/operatorImageMap.yaml run
