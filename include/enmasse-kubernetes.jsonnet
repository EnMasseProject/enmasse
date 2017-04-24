local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
{
  generate(use_tls, use_sasl, compact, with_kafka)::
  {
    local templateName = (if use_tls then "tls-enmasse-infra" else "enmasse-infra"),
    "apiVersion": "v1",
    "kind": "List",
    "items": [ templateConfig.generate(use_tls, use_sasl, compact, with_kafka, false),
               addressController.deployment(std.toString(use_tls), "enmasseproject/address-controller", "false", 0),
               restapiRoute.ingress(""),
               addressController.service,
               flavorConfig.generate(use_tls) ]
  }
}
