local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service::
    common.service("address-controller", "address-controller", "amqp", 5672, 55674),
  restapi::
    common.service("restapi", "address-controller", "http", 8080, 8080),
  deployment(image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "address-controller",
          "app": "enmasse"
        },
        "name": "address-controller"
      },
      "spec": {
        "replicas": 1,
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
              common.container("address-controller", image_repo, "amqp", 55674, "256Mi")
            ]
          }
        }
      }
    }
}
