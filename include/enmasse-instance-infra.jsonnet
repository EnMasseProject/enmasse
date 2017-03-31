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
local mqttGateway = import "mqtt-gateway.jsonnet";
local mqtt = import "mqtt.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local mqttRoute = import "mqtt-route.jsonnet";
local mqttLwt = import "mqtt-lwt.jsonnet";
local amqpKafkaBridge = import "amqp-kafka-bridge.jsonnet";
local amqpKafkaBridgeService = import "amqp-kafka-bridge-service.jsonnet";
local hawkularConfig = import "hawkular-broker-config.jsonnet";
{
  generate(use_tls, use_sasldb, compact, with_kafka)::
  {
    local templateName = (if use_tls then"tls-enmasse-instance-infra" else "enmasse-instance-infra"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": templateName
    },
    local common = [
      qdrouterd.deployment(use_tls, use_sasldb, "${INSTANCE}", "${ROUTER_REPO}"),
      messagingService.generate(use_tls, "${INSTANCE}", true),
      subserv.deployment("${INSTANCE}", "${SUBSERV_REPO}"),
      subserv.service("${INSTANCE}"),
      mqttGateway.deployment(use_tls, "${INSTANCE}", "${MQTT_GATEWAY_REPO}"),
      mqttService.generate(use_tls, "${INSTANCE}"),
      mqttLwt.deployment("${INSTANCE}", "${MQTT_LWT_REPO}"),
      hawkularConfig
    ],

    local compactAdmin = [
      admin.deployment(use_sasldb, "${INSTANCE}", "${CONFIGSERV_REPO}", "${RAGENT_REPO}", "${QUEUE_SCHEDULER_REPO}", "${CONSOLE_REPO}")
    ] + admin.services("${INSTANCE}"),

    local fullAdmin = [
      configserv.deployment("${INSTANCE}", "${CONFIGSERV_REPO}"),
      configserv.service("${INSTANCE}"),
      ragent.deployment("${INSTANCE}", "${RAGENT_REPO}"),
      ragent.service("${INSTANCE}"),
      queueScheduler.service("${INSTANCE}"),
      queueScheduler.deployment("${INSTANCE}", "${QUEUE_SCHEDULER_REPO}")
    ],

    local kafka = [
      amqpKafkaBridgeService.generate(use_tls, "${INSTANCE}"),
      amqpKafkaBridge.deployment("${INSTANCE}", "${AMQP_KAFKA_BRIDGE_REPO}")
    ],

    local securedRoutes = [ messagingRoute.generate("${INSTANCE}", "${MESSAGING_HOSTNAME}"), mqttRoute.generate("${INSTANCE}", "${MQTT_GATEWAY_HOSTNAME}") ],

    "objects": (if use_sasldb then [router.sasldb_pvc()] else []) + common + (if compact then compactAdmin else fullAdmin) + (if with_kafka then kafka else []) + (if use_tls then securedRoutes else []),

    "parameters": [
      {
        "name": "ROUTER_REPO",
        "description": "The image to use for the router",
        "value": "enmasseproject/qdrouterd"
      },
      {
        "name": "ROUTER_LINK_CAPACITY",
        "description": "The link capacity setting for router",
        "value": "50"
      },
      {
        "name": "CONFIGSERV_REPO",
        "description": "The image to use for the configuration service",
        "value": "enmasseproject/configserv"
      },
      {
        "name": "QUEUE_SCHEDULER_REPO",
        "description": "The docker image to use for the queue scheduler",
        "value": "enmasseproject/queue-scheduler"
      },
      {
        "name": "RAGENT_REPO",
        "description": "The image to use for the router agent",
        "value": "enmasseproject/ragent"
      },
      {
        "name": "SUBSERV_REPO",
        "description": "The image to use for the subscription services",
        "value": "enmasseproject/subserv"
      },
      {
        "name": "CONSOLE_REPO",
        "description": "The image to use for the console",
        "value": "enmasseproject/console"
      },
      {
        "name": "MESSAGING_HOSTNAME",
        "description": "The hostname to use for the exposed route for messaging (TLS only)"
      },
      {
        "name" : "MQTT_GATEWAY_REPO",
        "description": "The image to use for the MQTT gateway",
        "value": "enmasseproject/mqtt-gateway"
      },
      {
        "name": "MQTT_GATEWAY_HOSTNAME",
        "description": "The hostname to use for the exposed route for MQTT (TLS only)"
      },
      {
        "name" : "MQTT_LWT_REPO",
        "description": "The image to use for the MQTT LWT",
        "value": "enmasseproject/mqtt-lwt"
      },
      {
        "name" : "AMQP_KAFKA_BRIDGE_REPO",
        "description": "The image to use for the AMQP Kafka Bridge",
        "value": "enmasseproject/amqp-kafka-bridge"
      },
      {
        "name" : "KAFKA_BOOTSTRAP_SERVERS",
        "description": "A list of host/port pairs to use for establishing the initial connection to the Kafka cluster"
      },
      {
        "name": "INSTANCE",
        "description": "The instance this infrastructure is deployed for",
        "required": true
      }
    ]

  }
}
