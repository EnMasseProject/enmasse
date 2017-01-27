local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("address-controller", image_name),
  service::
    common.service("address-controller", "address-controller", "amqp", 5672, 55674),
  restapi::
    common.service("restapi", "address-controller", "http", 8080, 8080),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "address-controller",
          "app": "enmasse"
        },
        "name": "address-controller"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "address-controller"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          common.trigger("address-controller", "address-controller")
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "address-controller",
              "app": "enmasse"
            }
          },
          "spec": {
            "serviceAccount": "deployer",
            "containers": [
              common.container("address-controller", "address-controller", "amqp", 55674, "256Mi")
            ]
          }
        }
      }
    }
}
