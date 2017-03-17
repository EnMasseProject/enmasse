local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service(instance)::
    common.service(instance, "subscription", "subserv", "amqp", 5672, 5672),
  deployment(instance, container_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "name": "subserv",
        "instance": instance,
        "app": "enmasse"
      },
      "name": "subserv"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "subserv",
            "instance": instance,
            "app": "enmasse"
          }
        },
        "spec": {
          "containers": [
            common.container("subserv", container_image, "amqp", 5672, "64Mi", [])
          ]
        }
      }
    }
  }
}
