local secure = std.extVar("secure");
local templateName = (if secure == "true" then "enmasse-secure" else "enmasse");
local gen = import "broker-common.jsonnet";
local configmapBridge = import "configmap_bridge.json";
local ragent = import "ragent.json";
local qdrouterd = import "qdrouterd.json";
local storageController = import "storage_controller.json";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.json";
{
  "apiVersion": "v1",
  "kind": "Template",
  "metadata": {
    "name": templateName
  },
  "objects": [ gen.generate_template("false", "false"),
               gen.generate_template("false", "true"),
               gen.generate_template("true", "false"),
               gen.generate_template("true", "true") ] +
               configmapBridge +
               ragent +
               qdrouterd +
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
