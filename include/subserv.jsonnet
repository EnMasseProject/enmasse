local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("subserv", image_name),
  service::
    common.service("subscription", "subserv", "amqp", 5672, 5672),
  deployment::
  {
    "apiVersion": "v1",
    "kind": "DeploymentConfig",
    "metadata": {
      "labels": {
        "name": "subserv",
        "app": "enmasse"
      },
      "name": "subserv"
    },
    "spec": {
      "replicas": 1,
      "selector": {
        "name": "subserv"
      },
      "triggers": [
        {
          "type": "ConfigChange"
        },
        common.trigger("subserv", "subserv")
      ],
      "template": {
        "metadata": {
          "labels": {
            "name": "subserv",
            "app": "enmasse"
          }
        },
        "spec": {
          "containers": [
            common.container("subserv", "subserv", "amqp", 5672, "64Mi")
          ]
        }
      }
    }
  }
}
