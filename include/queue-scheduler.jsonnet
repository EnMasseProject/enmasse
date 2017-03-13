local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service(tenant)::
    common.service(tenant, "queue-scheduler", "queue-scheduler", "amqp", 55667, 55667),
  deployment(tenant, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "queue-scheduler",
          "app": "enmasse",
          "tenant": tenant
        },
        "name": tenant + "-queue-scheduler"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "queue-scheduler",
              "app": "enmasse",
              "tenant": tenant
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
