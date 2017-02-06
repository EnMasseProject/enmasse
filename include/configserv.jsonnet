local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service::
    common.service("configuration", "configserv", "amqp", 5672, 5672),
  deployment(image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "name": "configserv"
        },
        "name": "configserv"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "app": "enmasse",
              "name": "configserv"
            }
          },
          "spec": {
            "containers": [ common.container("configserv", image_repo, "amqp", 5672, "128Mi") ]
          }
        }
      }
    }
}
