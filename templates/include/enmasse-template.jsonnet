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
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "${MULTIINSTANCE}", ""),
                 addressController.internal_service,
                 authService.deployment("${KEYCLOAK_AUTHSERVICE_REPO}", "${NONE_AUTHSERVICE_REPO}"),
                 authService.none_authservice,
                 authService.keycloak_authservice,
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
        "name": "NONE_AUTHSERVICE_REPO",
        "description": "The docker image to use for the 'none' auth service'",
        "value": images.none_authservice
      },
      {
        "name": "KEYCLOAK_AUTHSERVICE_REPO",
        "description": "The docker image to use for the 'standard' auth service'",
        "value": images.keycloak_authservice
      }
    ]
  }
}
