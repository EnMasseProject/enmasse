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
      roles.event_reporter_role,
    ]
  },

  template::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "enmasse"
    },
    "objects": [ standardInfra.template(false, false),
                 standardInfra.template(false, true),
                 brokeredInfra.template,
                 addressController.deployment("${ADDRESS_CONTROLLER_IMAGE}", "", "${ADDRESS_CONTROLLER_CERT_SECRET}", "${ENVIRONMENT}", "${ENABLE_RBAC}", "${ENABLE_EVENT_LOGGER}", "${ADDRESS_CONTROLLER_SA}", "${ADDRESS_SPACE_ADMIN_SA}", "${WILDCARD_ENDPOINT_CERT_SECRET}", "${CONTROLLER_RESYNC_INTERVAL}", "${CONTROLLER_CHECK_INTERVAL}"),
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
        "name": "ADDRESS_CONTROLLER_IMAGE",
        "description": "The container image to use for the address controller",
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
      },
      {
        "name": "CONTROLLER_RESYNC_INTERVAL",
        "description": "Interval (in seconds) to use between controller resync",
        "value": "600"
      },
      {
        "name": "CONTROLLER_CHECK_INTERVAL",
        "description": "Interval (in seconds) to use between status checks",
        "value": "30"
      }
    ]
  }
}
