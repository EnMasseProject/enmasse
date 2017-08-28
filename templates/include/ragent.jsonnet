local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "ragent", "ragent", "amqp", 55671, 55671),
  deployment(addressSpace, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "ragent",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "ragent"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "ragent",
              "app": "enmasse"
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [
              common.container("ragent", image_repo, "amqp", 55671, "64Mi", [])
            ]
          }
        }
      }
    }
}
