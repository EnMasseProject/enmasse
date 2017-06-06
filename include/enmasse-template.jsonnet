local storage = import "storage-template.jsonnet";
local enmasseInfra = import "enmasse-instance-infra.jsonnet";
local addressController = import "address-controller.jsonnet";
local restapiRoute = import "restapi-route.jsonnet";
local flavorConfig = import "flavor.jsonnet";
local version = std.extVar("VERSION");
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
                 addressController.deployment("${ADDRESS_CONTROLLER_REPO}", "${MULTIINSTANCE}", "${ADDRESS_CONTROLLER_SECRET}", "", "${INSTANCE_IDLE_TIMEOUT_SECONDS}"),
                 addressController.internal_service,
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
        "value": "enmasseproject/address-controller:" + version
      },
      {
        "name": "ADDRESS_CONTROLLER_SECRET",
        "description": "The secret containing the certificate for the address-controller",
        "value": "enmasse-controller-certs"
      },
      {
        "name": "INSTANCE_CERT_SECRET",
        "description": "The secret to use to mount certificates. Only consumed if MULTIINSTANCE=false."
      },
      {
        "name": "INSTANCE_MESSAGING_HOST",
        "description": "The messaging host to use for this instance. Only consumed if MULTIINSTANCE=false."
      },
      {
        "name": "INSTANCE_MQTT_HOST",
        "description": "The mqtt host to use for this instance. Only consumed if MULTIINSTANCE=false."
      },
      {
        "name": "INSTANCE_CONSOLE_HOST",
        "description": "The console host to use for this instance. Only consumed if MULTIINSTANCE=false."
      }
    ]
  }
}
