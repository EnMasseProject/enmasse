local storage = import "storage-template.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
local enmasseInfra = import "enmasse-instance-infra.jsonnet";
{
  generate(secure, compact, with_kafka)::
  {
    local templateName = (if secure then "tls-enmasse-infra" else "enmasse-infra"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": templateName
    },
    "objects": [ storage.template(false, false, secure),
                 storage.template(false, true, secure),
                 storage.template(true, false, secure),
                 storage.template(true, true, secure),
                 enmasseInfra.generate(secure, compact, with_kafka),
                 addressController.deployment(std.toString(secure), "${ADDRESS_CONTROLLER_REPO}", "${MULTIINSTANCE}"),
                 addressController.service,
                 restapiRoute.generate("${RESTAPI_HOSTNAME}"),
                 flavorConfig.generate(secure) ],
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
      }
    ]
  }
}
