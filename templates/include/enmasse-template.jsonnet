local storage = import "storage-template.jsonnet";
local configmapBridge = import "configmap-bridge.jsonnet";
local ragent = import "ragent.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local storageController = import "storage-controller.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.json";
local template_router_image = "${ROUTER_IMAGE}";
{
  generate(secure)::
  {
    local templateName = (if secure then "enmasse-secure" else "enmasse"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "name": templateName
    },
    "objects": [ storage.template(false, false, secure, template_router_image),
                 storage.template(false, true, secure, template_router_image),
                 storage.template(true, false, secure, template_router_image),
                 storage.template(true, true, secure, template_router_image),
                 configmapBridge.imagestream("${CONFIGMAP_BRIDGE_IMAGE}"),
                 configmapBridge.deployment,
                 ragent.imagestream("${RAGENT_IMAGE}"),
                 ragent.deployment,
                 qdrouterd.generate(secure),
                 storageController.imagestream("${STORAGE_CONTROLLER_IMAGE}"),
                 storageController.deployment,
                 messagingService.generate(secure),
                 addressConfig,
                 flavorConfig,
                 import "ragent-service.json",
                 import "configuration-service.json" ],
    "parameters": [
      {
        "name": "ROUTER_IMAGE",
        "description": "The image to use for the router",
        "value": "gordons/qdrouterd:latest"
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
      }
    ]
    
  }
}
