local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "subscription", "subserv", "amqp", 5672, 5672),
  deployment(addressSpace, container_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "name": "subserv",
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": addressSpace
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
            "addressSpace": addressSpace
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
