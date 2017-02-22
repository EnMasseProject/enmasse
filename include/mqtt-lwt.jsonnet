local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  deployment(image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "mqtt-lwt",
          "app": "enmasse"
        },
        "name": "mqtt-lwt"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "mqtt-lwt",
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
