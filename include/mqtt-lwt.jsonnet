local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  deployment(tenant, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "mqtt-lwt",
          "tenant": tenant,
          "app": "enmasse"
        },
        "name": tenant + "-mqtt-lwt"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "mqtt-lwt",
              "tenant": tenant,
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              {
                "image": image_repo + ":" + version,
                "name": "mqtt-lwt"
              }
            ]
          }
        }
      }
    }
}
