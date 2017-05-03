local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
{
  generate(use_sasl, with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "enmasse"
    },
    "objects": [ templateConfig.generate(use_sasl, with_kafka, true),
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "${MULTIINSTANCE}", "${INSTANCE_IDLE_TIMEOUT_SECONDS}"),
                 addressController.service,
                 restapiRoute.route("${RESTAPI_HOSTNAME}"),
                 flavorConfig.generate() ],
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
        "name": "INSTANCE_IDLE_TIMEOUT_SECONDS",
        "description": "The time before garbage collecting idle (no addresses defined) instances",
        "value": "0"
      }
    ]
  }
}
