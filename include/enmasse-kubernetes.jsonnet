local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local consoleService = import "console-service.jsonnet";
local version = std.extVar("VERSION");
{
  common(use_sasl, with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ templateConfig.generate(use_sasl, with_kafka, false),
               addressController.deployment("enmasseproject/address-controller:" + version, "false", "enmasse-template-config", 0),
               restapiRoute.ingress(""),
               addressController.internal_service,
               flavorConfig.generate() ]
  },

  external_lb::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ 
      addressController.external_service,
      messagingService.external,
      mqttService.external,
      consoleService.external
    ]
  }
}
