local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("ragent", image_name),
  service::
    common.service("ragent", "ragent", "amqp", 55672, 55672),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "ragent",
          "app": "enmasse"
        },
        "name": "ragent"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "ragent"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          common.trigger("ragent", "ragent")
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "ragent",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              common.container("ragent", "ragent", "amqp", 55672, "64Mi")
            ]
          }
        }
      }
    }
}
