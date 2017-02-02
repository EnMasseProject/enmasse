local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("queue-scheduler", image_name),
  service::
    common.service("queue-scheduler", "queue-scheduler", "amqp", 55667, 55667),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "queue-scheduler",
          "app": "enmasse"
        },
        "name": "queue-scheduler"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "queue-scheduler"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          common.trigger("queue-scheduler", "queue-scheduler")
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "queue-scheduler",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              common.container("queue-scheduler", "queue-scheduler", "amqp", 55667, "128Mi")
            ]
          }
        }
      }
    }
}
