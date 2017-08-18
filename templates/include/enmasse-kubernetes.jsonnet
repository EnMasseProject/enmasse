local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local authService = import "auth-service.jsonnet";
local consoleService = import "console-service.jsonnet";
local images = import "images.jsonnet";
{
  common(use_sasl, with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ templateConfig.generate(use_sasl, with_kafka, false),
               authService.none_deployment(images.none_authservice),
               authService.none_authservice,
               addressController.deployment(images.address_controller, "false", "enmasse-template-config"),
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
