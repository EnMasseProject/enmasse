local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service(tenant)::
    common.service(tenant, "subscription", "subserv", "amqp", 5672, 5672),
  deployment(tenant, container_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "name": "subserv",
        "tenant": tenant,
        "app": "enmasse"
      },
      "name": tenant + "-subserv"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "subserv",
            "tenant": tenant,
            "app": "enmasse"
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
