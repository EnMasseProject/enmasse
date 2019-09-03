CMD=controller-manager
include ../Makefile.go.mk

build: build/operatorImageMap.yaml build/$(CMD)

build/operatorImageMap.yaml:
	echo "" > "$@"
	
	echo "iot-auth-service: $(IOT_AUTH_SERVICE_IMAGE)" >> "$@"
	echo "iot-device-registry-file: $(IOT_DEVICE_REGISTRY_FILE_IMAGE)" >> "$@"
	echo "iot-device-registry-infinispan: $(IOT_DEVICE_REGISTRY_INFINISPAN_IMAGE)" >> "$@"
	echo "iot-http-adapter: $(IOT_HTTP_ADAPTER_IMAGE)" >> "$@"
	echo "iot-mqtt-adapter: $(IOT_MQTT_ADAPTER_IMAGE)" >> "$@"
	echo "iot-lorawan-adapter: $(IOT_LORAWAN_ADAPTER_IMAGE)" >> "$@"
	echo "iot-sigfox-adapter: $(IOT_SIGFOX_ADAPTER_IMAGE)" >> "$@"
	echo "iot-tenant-service: $(IOT_TENANT_SERVICE_IMAGE)" >> "$@"
	echo "iot-proxy-configurator: $(IOT_PROXY_CONFIGURATOR_IMAGE)" >> "$@"
	echo "qdrouterd-base: $(QDROUTERD_BASE_IMAGE)" >> "$@"
	echo "none-authservice: $(NONE_AUTHSERVICE_IMAGE)" >> "$@"
	echo "keycloak: $(KEYCLOAK_IMAGE)" >> "$@"
	echo "keycloak-plugin: $(KEYCLOAK_PLUGIN_IMAGE)" >> "$@"
	echo "console-init: $(CONSOLE_INIT_IMAGE)" >> "$@"
	echo "console-proxy-openshift: $(CONSOLE_PROXY_OPENSHIFT_IMAGE)" >> "$@"
	echo "console-proxy-kubernetes: $(CONSOLE_PROXY_KUBERNETES_IMAGE)" >> "$@"
	echo "console-httpd: $(CONSOLE_HTTPD_IMAGE)" >> "$@"

.PHONY: build/operatorImageMap.yaml
