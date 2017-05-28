local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
{
  common(use_sasl, with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ templateConfig.generate(use_sasl, with_kafka, false),
               addressController.deployment("enmasseproject/address-controller", "false", 0),
               restapiRoute.ingress(""),
               addressController.internal_service,
               flavorConfig.generate() ]
  },

  external_lb(use_sasl, with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ 
      addressController.external_service
    ]
  }
}
