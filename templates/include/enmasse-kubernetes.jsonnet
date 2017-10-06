local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local common = import "common.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local consoleService = import "console-service.jsonnet";
local images = import "images.jsonnet";
{
  common(with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ templateConfig.generate(with_kafka),
               addressController.deployment(images.address_controller, "enmasse-template-config", "enmasse-ca", "address-controller-cert", "false"),
               common.empty_secret("address-controller-userdb"),
               restapiRoute.ingress(""),
               addressController.internal_service ]
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
