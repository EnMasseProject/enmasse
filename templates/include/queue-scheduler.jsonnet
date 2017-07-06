local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "queue-scheduler", "queue-scheduler", "amqp", 55667, 55667),
  deployment(addressSpace, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "queue-scheduler",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "queue-scheduler"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "queue-scheduler",
              "app": "enmasse"
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [
              common.container("queue-scheduler", image_repo, "amqp", 55667, "128Mi", [])
            ]
          }
        }
      }
    }
}
