local templateConfig = import "template-config.jsonnet";
local addressController = import "address-controller.jsonnet";
local common = import "common.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local consoleService = import "console-service.jsonnet";
local images = import "images.jsonnet";
local roles = import "roles.jsonnet";
{
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

  list::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [ templateConfig.global,
               addressController.deployment(images.address_controller, "enmasse-template-config", "address-controller-cert", "development", "false", "false", "enmasse-admin", "address-space-admin", "", "600", "30"),
               addressController.internal_service ]
  },

  external_lb::
    addressController.external_service,
}
