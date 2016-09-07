local secure = std.extVar("secure");
local templateName = (if secure == "true" then "enmasse-secure" else "enmasse");
local storage = import "storage.jsonnet";
local configmapBridge = import "configmap_bridge.json";
local ragent = import "ragent.json";
local qdrouterd = import "qdrouterd.jsonnet";
local storageController = import "storage_controller.json";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.json";
{
  "apiVersion": "v1",
  "kind": "Template",
  "metadata": {
    "name": templateName
  },
  "objects": [ storage.template("false", "false", secure),
               storage.template("false", "true", secure),
               storage.template("true", "false", secure),
               storage.template("true", "true", secure) ] +
               configmapBridge +
               ragent +
               qdrouterd.generate(secure) +
               storageController +
               addressConfig +
               flavorConfig,
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
