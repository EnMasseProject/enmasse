local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service::
    common.service("ragent", "ragent", "amqp", 55672, 55672),
  deployment(image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "ragent",
          "app": "enmasse"
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
            }
          },
          "spec": {
            "containers": [
              common.container("ragent", image_repo, "amqp", 55672, "64Mi")
            ]
          }
        }
      }
    }
}
