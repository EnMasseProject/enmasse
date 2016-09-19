local storage = import "storage-template.jsonnet";
local configmapBridge = import "configmap-bridge.jsonnet";
local ragent = import "ragent.jsonnet";
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local storageController = import "storage-controller.jsonnet";
local subserv = import "subserv.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.json";
{
  generate(secure, with_storage_controller)::
  {
    local templateName = (if secure then "enmasse-secure" else "enmasse"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "name": templateName
    },
    local common = [ storage.template(false, false, secure),
                 storage.template(false, true, secure),
                 storage.template(true, false, secure),
                 storage.template(true, true, secure),
                 configmapBridge.imagestream("${CONFIGMAP_BRIDGE_IMAGE}"),
                 configmapBridge.deployment,
                 ragent.imagestream("${RAGENT_IMAGE}"),
                 ragent.deployment,
                 router.imagestream("${ROUTER_IMAGE}"),
                 qdrouterd.deployment(secure),
                 broker.imagestream("${BROKER_IMAGE}"),
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
        "value": "gordons/qdrouterd:latest"
      },
      {
        "name": "BROKER_IMAGE",
        "description": "The default image to use as broker",
        "value": "enmasseproject/artemis:latest"
      },
      {
        "name": "ROUTER_LINK_CAPACITY",
        "description": "The link capacity setting for router",
        "value": "50"
      },
      {
        "name": "CONFIGMAP_BRIDGE_IMAGE",
        "description": "The image to use for the configmap notification bridge",
        "value": "enmasseproject/configmap-bridge:latest"
      },
      {
        "name": "STORAGE_CONTROLLER_IMAGE",
        "description": "The docker image to use for the storage controller",
        "value": "enmasseproject/storage-controller:latest"
      },
      {
        "name": "RAGENT_IMAGE",
        "description": "The image to use for the router agent",
        "value": "enmasseproject/ragent:latest"
      },
      {
        "name": "SUBSERV_IMAGE",
        "description": "The image to use for the subscription services",
        "value": "enmasseproject/subserv:latest"
      }
    ]

  }
}
