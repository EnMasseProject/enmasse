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
        "app": "enmasse"
      },
      "annotations": {
        "instance": instance
      },
      "name": "subserv"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "subserv",
            "app": "enmasse"
          },
          "annotations": {
            "instance": instance
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
