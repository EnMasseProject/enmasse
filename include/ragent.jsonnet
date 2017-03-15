local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service(instance)::
    common.service(instance, "ragent", "ragent", "amqp", 55672, 55672),
  deployment(instance, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "ragent",
          "app": "enmasse",
          "instance": instance
        },
        "name": instance + "-ragent"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "ragent",
              "instance": instance,
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              common.container("ragent", image_repo, "amqp", 55672, "64Mi", [])
            ]
          }
        }
      }
    }
}
