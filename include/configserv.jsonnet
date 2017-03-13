local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service(tenant)::
    common.service(tenant, "configuration", "configserv", "amqp", 5672, 5672),
  deployment(tenant, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "name": "configserv",
          "tenant": tenant
        },
        "name": tenant + "-configserv"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "app": "enmasse",
              "name": "configserv",
              "tenant": tenant
            }
          },
          "spec": {
            "containers": [ common.container("configserv", image_repo, "amqp", 5672, "128Mi", []) ]
          }
        }
      }
    }
}
