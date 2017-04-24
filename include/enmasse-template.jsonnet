local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
{
  generate(use_tls, use_sasl, compact, with_kafka)::
  {
    local templateName = (if use_tls then "tls-enmasse-infra" else "enmasse-infra"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": templateName
    },
    "objects": [ templateConfig.generate(use_tls, use_sasl, compact, with_kafka, true),
                 addressController.deployment(std.toString(use_tls), "${ADDRESS_CONTROLLER_REPO}", "${MULTIINSTANCE}", "${INSTANCE_IDLE_TIMEOUT}"),
                 addressController.service,
                 restapiRoute.route("${RESTAPI_HOSTNAME}"),
                 flavorConfig.generate(use_tls) ],
    "parameters": [
      {
        "name": "RESTAPI_HOSTNAME",
        "description": "The hostname to use for the exposed route for the REST API"
      },
      {
        "name": "MULTIINSTANCE",
        "description": "If set to true, the address controller will deploy infrastructure to separate namespaces",
        "value": "false"
      },
      {
        "name": "ADDRESS_CONTROLLER_REPO",
        "description": "The docker image to use for the address controller",
        "value": "enmasseproject/address-controller"
      },
      {
        "name": "INSTANCE_IDLE_TIMEOUT",
        "description": "The time before garbage collecting idle (no addresses defined) instances",
        "value": "0"
      }
    ]
  }
}
