local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service::
    common.service("queue-scheduler", "queue-scheduler", "amqp", 55667, 55667),
  deployment(image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "queue-scheduler",
          "app": "enmasse"
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
            }
          },
          "spec": {
            "containers": [
              common.container("queue-scheduler", image_repo, "amqp", 55667, "128Mi")
            ]
          }
        }
      }
    }
}
