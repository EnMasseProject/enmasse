local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service(instance)::
    common.service(instance, "queue-scheduler", "queue-scheduler", "amqp", 55667, 55667),
  deployment(instance, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "queue-scheduler",
          "app": "enmasse"
        },
        "annotations": {
          "instance": instance
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
              "instance": instance
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
