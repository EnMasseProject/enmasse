local storage = import "storage-template.jsonnet";
local enmasseInfra = import "enmasse-instance-infra.jsonnet";
local addressController = import "address-controller.jsonnet";
local authService = import "auth-service.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local images = import "images.jsonnet";
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
    "objects": [ storage.template(false, false),
                 storage.template(false, true),
                 storage.template(true, false),
                 storage.template(true, true),
                 enmasseInfra.generate(use_sasl, with_kafka, true),
                 authService.none_deployment("${NONE_AUTHSERVICE_IMAGE}"),
                 authService.none_authservice,
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "${MULTIINSTANCE}", ""),
                 addressController.internal_service,
                 restapiRoute.route("${RESTAPI_HOSTNAME}") ],
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
        "value": images.address_controller
      },
      {
        "name": "NONE_AUTHSERVICE_IMAGE",
        "description": "The docker image to use for the 'none' auth service",
        "value": images.none_authservice
      }
    ]
  }
}
