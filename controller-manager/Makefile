CMD=controller-manager
include ../Makefile.go.mk

build: build/operatorImageMap.yaml build/$(CMD)

build/operatorImageMap.yaml:
	echo "" > "$@"
	
	echo "iot-auth-service: $(IOT_AUTH_SERVICE_IMAGE)" >> "$@"
	echo "iot-device-registry-file: $(IOT_DEVICE_REGISTRY_FILE_IMAGE)" >> "$@"
	echo "iot-gc: $(IOT_GC_IMAGE)" >> "$@"
	echo "iot-http-adapter: $(IOT_HTTP_ADAPTER_IMAGE)" >> "$@"
	echo "iot-mqtt-adapter: $(IOT_MQTT_ADAPTER_IMAGE)" >> "$@"
	echo "iot-tenant-service: $(IOT_TENANT_SERVICE_IMAGE)" >> "$@"
	echo "iot-proxy-configurator: $(IOT_PROXY_CONFIGURATOR_IMAGE)" >> "$@"
	echo "qdrouterd-base: $(QDROUTERD_BASE_IMAGE)" >> "$@"
	echo "none-authservice: $(NONE_AUTHSERVICE_IMAGE)" >> "$@"
	echo "keycloak: $(KEYCLOAK_IMAGE)" >> "$@"
	echo "keycloak-plugin: $(KEYCLOAK_PLUGIN_IMAGE)" >> "$@"

.PHONY: build/operatorImageMap.yaml
