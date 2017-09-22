local configserv = import "configserv.jsonnet";
local ragent = import "ragent.jsonnet";
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local queueScheduler = import "queue-scheduler.jsonnet";
local subserv = import "subserv.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local messagingRoute = import "messaging-route.jsonnet";
local common = import "common.jsonnet";
local admin = import "admin.jsonnet";
local console = import "console.jsonnet";
local mqttGateway = import "mqtt-gateway.jsonnet";
local mqtt = import "mqtt.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local mqttRoute = import "mqtt-route.jsonnet";
local mqttLwt = import "mqtt-lwt.jsonnet";
local amqpKafkaBridge = import "amqp-kafka-bridge.jsonnet";
local amqpKafkaBridgeService = import "amqp-kafka-bridge-service.jsonnet";
local hawkularBrokerConfig = import "hawkular-broker-config.jsonnet";
local hawkularRouterConfig = import "hawkular-router-config.jsonnet";
local images = import "images.jsonnet";

{
  generate(with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "enmasse-instance-infra"
    },
    local common_items = admin.services("${ADDRESS_SPACE}") + [
      messagingService.internal("${ADDRESS_SPACE}"),
      subserv.service("${ADDRESS_SPACE}"),
      mqttService.internal("${ADDRESS_SPACE}"),
      qdrouterd.deployment("${ADDRESS_SPACE}", "${ROUTER_REPO}", "${ROUTER_METRICS_REPO}", "${ROUTER_SECRET}", "authservice-ca", "address-controller-ca"),
      subserv.deployment("${ADDRESS_SPACE}", "${SUBSERV_REPO}"),
      mqttGateway.deployment("${ADDRESS_SPACE}", "${MQTT_GATEWAY_REPO}", "${MQTT_SECRET}"),
      mqttLwt.deployment("${ADDRESS_SPACE}", "${MQTT_LWT_REPO}"),
      common.ca_secret("authservice-ca", "${AUTHENTICATION_SERVICE_CA_CERT}"),
      common.ca_secret("address-controller-ca", "${ADDRESS_CONTROLLER_CA_CERT}"),
      hawkularBrokerConfig,
      hawkularRouterConfig,
      admin.deployment("${ADDRESS_SPACE}", "${CONFIGSERV_REPO}", "${RAGENT_REPO}", "${QUEUE_SCHEDULER_REPO}", "${CONSOLE_REPO}", "authservice-ca", "address-controller-ca"),
      admin.password_secret("address-space-credentials", "${ADDRESS_SPACE_PASSWORD}"),
    ],

    local kafka = [
      amqpKafkaBridgeService.generate("${ADDRESS_SPACE}"),
      amqpKafkaBridge.deployment("${ADDRESS_SPACE}", "${AMQP_KAFKA_BRIDGE_REPO}")
    ],

    "objects":
      common_items +
      (if with_kafka then kafka else []),

    local commonParameters = [
      {
        "name": "ADDRESS_SPACE_SERVICE_HOST",
        "description": "Hostname where API server can be reached",
        "value": ""
      },
      {
        "name": "ROUTER_REPO",
        "description": "The image to use for the router",
        "value": images.router
      },
      {
        "name": "ROUTER_METRICS_REPO",
        "description": "The image to use for the router metrics collector",
        "value": images.router_metrics
      },
      {
        "name": "ROUTER_LINK_CAPACITY",
        "description": "The link capacity setting for router",
        "value": "50"
      },
      {
        "name": "CONFIGSERV_REPO",
        "description": "The image to use for the configuration service",
        "value": images.configserv
      },
      {
        "name": "QUEUE_SCHEDULER_REPO",
        "description": "The docker image to use for the queue scheduler",
        "value": images.queue_scheduler
      },
      {
        "name": "RAGENT_REPO",
        "description": "The image to use for the router agent",
        "value": images.ragent
      },
      {
        "name": "SUBSERV_REPO",
        "description": "The image to use for the subscription services",
        "value": images.subserv
      },
      {
        "name": "CONSOLE_REPO",
        "description": "The image to use for the console",
        "value": images.console
      },
      {
        "name": "MESSAGING_HOSTNAME",
        "description": "The hostname to use for the exposed route for messaging"
      },
      {
        "name" : "MQTT_GATEWAY_REPO",
        "description": "The image to use for the MQTT gateway",
        "value": images.mqtt_gateway
      },
      {
        "name": "MQTT_GATEWAY_HOSTNAME",
        "description": "The hostname to use for the exposed route for MQTT"
      },
      {
        "name": "CONSOLE_HOSTNAME",
        "description": "The hostname to use for the exposed route for the messaging console"
      },
      {
        "name": "ROUTER_SECRET",
        "description": "The secret to mount for router private key and certificate",
        "required": true
      },
      {
        "name": "MQTT_SECRET",
        "description": "The secret to mount for MQTT private key and certificate",
        "required": true
      },
      {
        "name" : "MQTT_LWT_REPO",
        "description": "The image to use for the MQTT LWT",
        "value": images.mqtt_lwt
      },
      {
        "name": "ADDRESS_SPACE",
        "description": "The address space this infrastructure is deployed for",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_HOST",
        "description": "The hostname of the authentication service used by this address space",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_PORT",
        "description": "The port of the authentication service used by this address space",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_CA_CERT",
        "description": "The CA cert to use for validating authentication service cert",
      },
      {
        "name": "AUTHENTICATION_SERVICE_CLIENT_SECRET",
        "description": "The client cert to use as identity against authentication service",
      },
      {
        "name": "AUTHENTICATION_SERVICE_SASL_INIT_HOST",
        "description": "The hostname to use in sasl init",
      },
      {
        "name": "ADDRESS_CONTROLLER_CA_CERT",
        "description": "The CA cert to use for validating address controller identity"
      },
      {
        "name": "ADDRESS_SPACE_PASSWORD",
        "description": "Password for authenticating against address controller"
      },
      {
        "name" : "AMQP_KAFKA_BRIDGE_REPO",
        "description": "The image to use for the AMQP Kafka Bridge",
        "value": images.amqp_kafka_bridge
      },
      {
        "name" : "KAFKA_BOOTSTRAP_SERVERS",
        "description": "A list of host/port pairs to use for establishing the initial connection to the Kafka cluster"
      }
    ],
    "parameters": commonParameters
  }
}
