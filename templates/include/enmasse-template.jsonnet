local storage = import "storage-template.jsonnet";
local common = import "common.jsonnet";
local standardInfra = import "standard-space-infra.jsonnet";
local brokeredInfra = import "brokered-space-infra.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local images = import "images.jsonnet";
{
  enmasse_admin_role::
    {
      "apiVersion": "v1",
      "kind": "Role",
      "metadata": {
        "name": "enmasse-admin"
      },
      "rules": [
        {
          "resources": [
            "configmaps"
          ],
          "verbs": [
            "get",
            "list",
            "watch",
            "create",
            "update",
            "patch",
            "delete"
          ]
        }
      ]
    },

  local me = self,

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
                 me.enmasse_admin_role,
                 brokeredInfra.template,
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "", "${ENMASSE_CA_SECRET}", "${ADDRESS_CONTROLLER_CERT_SECRET}"),
                 addressController.internal_service,
                 restapiRoute.route("${RESTAPI_HOSTNAME}") ],
    "parameters": [
      {
        "name": "RESTAPI_HOSTNAME",
        "description": "The hostname to use for the exposed route for the REST API"
      },
      {
        "name": "ADDRESS_CONTROLLER_REPO",
        "description": "The docker image to use for the address controller",
        "value": images.address_controller
      },
      {
        "name": "ENMASSE_CA_SECRET",
        "description": "Name of the secret containing the EnMasse CA",
        "value": "enmasse-ca"
      },
      {
        "name": "ADDRESS_CONTROLLER_CERT_SECRET",
        "description": "Name of the secret containing the address controller certificate",
        "value": "address-controller-cert"
      }
    ]
  }
}
