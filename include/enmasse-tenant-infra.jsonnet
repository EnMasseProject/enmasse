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
{
  generate(secure, compact, with_kafka)::
  {
    local templateName = (if secure then "tls-enmasse-tenant-infra" else "enmasse-tenant-infra"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": templateName
    },
    local common = [
      qdrouterd.deployment(secure, "${TENANT}", "${ROUTER_REPO}"),
      messagingService.generate(secure, "${TENANT}", true),
      subserv.deployment("${TENANT}", "${SUBSERV_REPO}"),
      subserv.service("${TENANT}"),
      mqttGateway.deployment(secure, "${TENANT}", "${MQTT_GATEWAY_REPO}"),
      mqttService.generate(secure, "${TENANT}"),
      mqttLwt.deployment("${TENANT}", "${MQTT_LWT_REPO}")
    ],

    local compactAdmin = [
      admin.deployment("${TENANT}", "${CONFIGSERV_REPO}", "${RAGENT_REPO}", "${QUEUE_SCHEDULER_REPO}")
    ] + admin.services("${TENANT}"),

    local fullAdmin = [
      configserv.deployment("${TENANT}", "${CONFIGSERV_REPO}"),
      configserv.service("${TENANT}"),
      ragent.deployment("${TENANT}", "${RAGENT_REPO}"),
      ragent.service("${TENANT}"),
      queueScheduler.service("${TENANT}"),
      queueScheduler.deployment("${TENANT}", "${QUEUE_SCHEDULER_REPO}")
    ],

    local kafka = [ 
      amqpKafkaBridgeService.generate(secure, "${TENANT}"),
      amqpKafkaBridge.deployment("${TENANT}", "${AMQP_KAFKA_BRIDGE_REPO}")
    ],

    local securedRoutes = [ messagingRoute.generate("${TENANT}", "${MESSAGING_HOSTNAME}"), mqttRoute.generate("${TENANT}", "${MQTT_GATEWAY_HOSTNAME}") ],

    "objects": common + 
      if compact then compactAdmin else fullAdmin +
      if with_kafka then kafka else [] + 
      if secure then securedRoutes else [],

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
        "name": "TENANT",
        "description": "The tenant this infrastructure is deployed for",
        "required": true
      }
    ]

  }
}
