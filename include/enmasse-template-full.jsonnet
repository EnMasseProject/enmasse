local storage = import "storage-template.jsonnet";
local configserv = import "configserv.jsonnet";
local ragent = import "ragent.jsonnet";
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local addressController = import "address-controller.jsonnet";
local queueScheduler = import "queue-scheduler.jsonnet";
local subserv = import "subserv.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local messagingRoute = import "messaging-route.json";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.jsonnet";
local common = import "common.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local mqttGateway = import "mqtt-gateway.jsonnet";
local mqtt = import "mqtt.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local mqttRoute = import "mqtt-route.json";
local mqttLwt = import "mqtt-lwt.jsonnet";
{
  generate(secure, with_address_controller)::
  {
    local templateName = (if secure then "tls-enmasse" else "enmasse") + (if with_address_controller then "" else "-base"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": templateName
    },
    local common = [ storage.template(false, false, secure),
                 storage.template(false, true, secure),
                 storage.template(true, false, secure),
                 storage.template(true, true, secure),
                 import "direct-template.json",
                 restapiRoute.generate(false),

                 qdrouterd.deployment(secure, "${ROUTER_REPO}"),
                 messagingService.generate(secure, false),
                 configserv.deployment("${CONFIGSERV_REPO}"),
                 configserv.service,
                 ragent.deployment("${RAGENT_REPO}"),
                 ragent.service,
                 subserv.deployment("${SUBSERV_REPO}"),
                 subserv.service,
                 queueScheduler.service,
                 queueScheduler.deployment("${QUEUE_SCHEDULER_REPO}"),
                 mqttGateway.deployment(secure, "${MQTT_GATEWAY_REPO}"),
                 mqttService.generate(secure),
                 mqttLwt.deployment("${MQTT_LWT_REPO}") ],
    local address_controller_resources = [
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}"),
                 addressController.service,
                 addressController.restapi,
                 flavorConfig.generate(secure)],

    local all_objects = if with_address_controller then common + address_controller_resources else common,
    local secured_objects = all_objects + [ messagingRoute, mqttRoute ],
    "objects": if secure then secured_objects else all_objects,
    "parameters": [
      {
        "name": "ROUTER_REPO",
        "description": "The image to use for the router",
        "value": "enmasseproject/qdrouterd"
      },
      {
        "name": "BROKER_REPO",
        "description": "The default image to use as broker",
        "value": "enmasseproject/artemis"
      },
      {
        "name": "TOPIC_FORWARDER_REPO",
        "description": "The default image to use as topic forwarder",
        "value": "enmasseproject/topic-forwarder"
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
        "name": "ADDRESS_CONTROLLER_REPO",
        "description": "The docker image to use for the address controller",
        "value": "enmasseproject/address-controller"
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
        "name": "RESTAPI_HOSTNAME",
        "description": "The hostname to use for the exposed route for the REST API"
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
      }
    ]

  }
}
