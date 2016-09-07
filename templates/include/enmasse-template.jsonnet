local storage = import "storage.jsonnet";
local configmapBridge = import "configmap-bridge.jsonnet";
local ragent = import "ragent.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local storageController = import "storage-controller.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.json";
{
  generate(secure)::
  {
    local templateName = (if secure then "enmasse-secure" else "enmasse"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "name": templateName
    },
    "objects": [ storage.template(false, false, secure),
                 storage.template(false, true, secure),
                 storage.template(true, false, secure),
                 storage.template(true, true, secure),
                 configmapBridge.generate("${CONFIGMAP_BRIDGE_IMAGE}"),
                 ragent.generate("${RAGENT_IMAGE}"),
                 qdrouterd.generate(secure),
                 storageController.generate("${STORAGE_CONTROLLER_IMAGE}"),
                 messagingService.generate(secure),
                 addressConfig,
                 flavorConfig,
                 import "ragent-service.json",
                 import "configuration-service.json" ],
    "parameters": [
      {
        "name": "QDROUTER_IMAGE",
        "description": "The image to use for the router",
        "value": "gordons/qdrouterd:latest"
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
