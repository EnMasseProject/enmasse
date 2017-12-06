local storage = import "storage-template.jsonnet";
local common = import "common.jsonnet";
local standardInfra = import "standard-space-infra.jsonnet";
local brokeredInfra = import "brokered-space-infra.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local images = import "images.jsonnet";
local roles = import "roles.jsonnet";
{
  local me = self,

  cluster_roles::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      roles.address_admin_role,
      roles.namespace_admin_role,
      roles.addressspace_admin_role,
      roles.infra_view_role
    ]
  },

  generate(with_kafka)::
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
                 standardInfra.generate(with_kafka),
                 brokeredInfra.template,
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "", "${ADDRESS_CONTROLLER_CERT_SECRET}", "${ENVIRONMENT}", "${ENABLE_RBAC}", "${ADDRESS_CONTROLLER_SA}", "${ADDRESS_SPACE_ADMIN_SA}"),
                 addressController.internal_service,
                 restapiRoute.route("${RESTAPI_HOSTNAME}") ],
    "parameters": [
      {
        "name": "RESTAPI_HOSTNAME",
        "description": "The hostname to use for the exposed route for the REST API"
      },
      {
        "name": "ADDRESS_CONTROLLER_SA",
        "description": "The service account to use for the address controller",
        "value": "enmasse-admin"
      },
      {
        "name": "ADDRESS_CONTROLLER_REPO",
        "description": "The docker image to use for the address controller",
        "value": images.address_controller
      },
      {
        "name": "ADDRESS_CONTROLLER_CERT_SECRET",
        "description": "Name of the secret containing the address controller certificate",
        "value": "address-controller-cert"
      },
      {
        "name": "ENABLE_RBAC",
        "description": "Enable RBAC for REST API authentication and authorization",
        "value": "false"
      },
      {
        "name": "ENVIRONMENT",
        "description": "The environment for this EnMasse instance (for instance development, testing or production).",
        "value": "development"
      },
      {
        "name": "ADDRESS_SPACE_ADMIN_SA",
        "description": "The service account with address space admin privileges",
        "value": "address-space-admin"
      }
    ]
  }
}
