local storage = import "storage-template.jsonnet";
local configserv = import "configserv.jsonnet";
local ragent = import "ragent.jsonnet";
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local storageController = import "storage-controller.jsonnet";
local subserv = import "subserv.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.json";
{
  generate(secure, with_storage_controller, version)::
  {
    local templateName = (if secure then "tls-enmasse" else "enmasse") + (if with_storage_controller then "" else "-base"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "name": templateName
    },
    local common = [ storage.template(false, false, secure),
                 storage.template(false, true, secure),
                 storage.template(true, false, secure),
                 storage.template(true, true, secure),
                 import "direct-template.json",
                 configserv.imagestream("${CONFIGSERV_IMAGE}"),
                 configserv.deployment,
                 ragent.imagestream("${RAGENT_IMAGE}"),
                 ragent.deployment,
                 router.imagestream("${ROUTER_IMAGE}"),
                 qdrouterd.deployment(secure),
                 broker.imagestream("${BROKER_IMAGE}"),
                 forwarder.imagestream("${TOPIC_FORWARDER_IMAGE}"),
                 subserv.imagestream("${SUBSERV_IMAGE}"),
                 subserv.deployment,
                 messagingService.generate(secure),
                 import "ragent-service.json",
                 import "configuration-service.json",
                 import "subscription-service.json"],
    local storage_controller_resources = [
                 storageController.imagestream("${STORAGE_CONTROLLER_IMAGE}"),
                 storageController.deployment,
                 addressConfig,
                 flavorConfig],
    "objects": if with_storage_controller then common + storage_controller_resources else common,
    "parameters": [
      {
        "name": "ROUTER_IMAGE",
        "description": "The image to use for the router",
        "value": "gordons/qdrouterd:" + version
      },
      {
        "name": "BROKER_IMAGE",
        "description": "The default image to use as broker",
        "value": "enmasseproject/artemis:" + version
      },
      {
        "name": "TOPIC_FORWARDER_IMAGE",
        "description": "The default image to use as topic forwarder",
        "value": "enmasseproject/topic-forwarder:" + version
      },
      {
        "name": "ROUTER_LINK_CAPACITY",
        "description": "The link capacity setting for router",
        "value": "50"
      },
      {
        "name": "CONFIGSERV_IMAGE",
        "description": "The image to use for the configuration service",
        "value": "enmasseproject/configserv:" + version
      },
      {
        "name": "STORAGE_CONTROLLER_IMAGE",
        "description": "The docker image to use for the storage controller",
        "value": "enmasseproject/storage-controller:" + version
      },
      {
        "name": "RAGENT_IMAGE",
        "description": "The image to use for the router agent",
        "value": "enmasseproject/ragent:" + version
      },
      {
        "name": "SUBSERV_IMAGE",
        "description": "The image to use for the subscription services",
        "value": "enmasseproject/subserv:" + version
      },
      {
        "name": "ENMASSE_VERSION",
        "description": "EnMasse version",
        "value": version
      }
    ]

  }
}
