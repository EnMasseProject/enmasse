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
	echo "router: $(ROUTER_IMAGE)" >> "$@"
	echo "none-authservice: $(NONE_AUTHSERVICE_IMAGE)" >> "$@"
	echo "keycloak: $(KEYCLOAK_IMAGE)" >> "$@"
	echo "keycloak-plugin: $(KEYCLOAK_PLUGIN_IMAGE)" >> "$@"
	echo "console-init: $(CONSOLE_INIT_IMAGE)" >> "$@"
	echo "console-server: $(CONSOLE_SERVER_IMAGE)" >> "$@"
	echo "console-proxy-openshift: $(CONSOLE_PROXY_OPENSHIFT_IMAGE)" >> "$@"
	echo "console-proxy-openshift3: $(CONSOLE_PROXY_OPENSHIFT3_IMAGE)" >> "$@"
	echo "console-proxy-kubernetes: $(CONSOLE_PROXY_KUBERNETES_IMAGE)" >> "$@"
	echo "standard-controller: $(STANDARD_CONTROLLER_IMAGE)" >> "$@"
	echo "agent: $(AGENT_IMAGE)" >> "$@"
	echo "broker: $(BROKER_IMAGE)" >> "$@"
	echo "broker-plugin: $(BROKER_PLUGIN_IMAGE)" >> "$@"
	echo "topic-forwarder: $(TOPIC_FORWARDER_IMAGE)" >> "$@"

.PHONY: build/operatorImageMap.yaml run
