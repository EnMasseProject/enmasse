local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("storage-controller", image_name),
  service::
    common.service("storage-controller", "storage-controller", "amqp", 5672, 55674),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "storage-controller",
          "app": "enmasse"
        },
        "name": "storage-controller"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "storage-controller"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          common.trigger("storage-controller", "storage-controller")
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "storage-controller",
              "app": "enmasse"
            }
          },
          "spec": {
            "serviceAccount": "deployer",
            "containers": [
              common.container("storage-controller", "storage-controller", "amqp", 55674)
            ]
          }
        }
      }
    }
}
