include ../Makefile.common

CMD=enmasse-controller-manager

build/$(CMD):
	cd $(GOPRJ)/cmd/$(@F) && GOOS=$(BUILD_GOOS) GOARCH=$(BUILD_GOARCH) go build -o $(abspath $@) .

build: build/operatorImageMap.yaml build/$(CMD)

package:
	@echo "Nothing to package"

build/operatorImageMap.yaml:
	echo "" > "$@"
	
	echo "iot-auth-service: $(IOT_AUTH_SERVICE_IMAGE)" >> "$@"
	echo "iot-device-registry: $(IOT_DEVICE_REGISTRY_IMAGE)" >> "$@"
	echo "iot-gc: $(IOT_GC_IMAGE)" >> "$@"
	echo "iot-http-adapter: $(IOT_HTTP_ADAPTER_IMAGE)" >> "$@"
	echo "iot-mqtt-adapter: $(IOT_MQTT_ADAPTER_IMAGE)" >> "$@"
	echo "iot-tenant-service: $(IOT_TENANT_SERVICE_IMAGE)" >> "$@"
	echo "qdr-proxy-configurator: $(QDR_PROXY_CONFIGURATOR_IMAGE)" >> "$@"
	echo "none-authservice: $(NONE_AUTHSERVICE_IMAGE)" >> "$@"
	echo "keycloak: $(KEYCLOAK_IMAGE)" >> "$@"
	echo "keycloak-plugin: $(KEYCLOAK_PLUGIN_IMAGE)" >> "$@"

test:

.PHONY: build/$(CMD) build/operatorImageMap.yaml
