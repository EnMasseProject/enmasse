local common = import "common.jsonnet";
{
  deployment(instance, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "mqtt-lwt",
          "app": "enmasse"
        },
        "annotations": {
          "instance": instance
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
            },
            "annotations": {
              "instance": instance
            }
          },
          "spec": {
            "containers": [
              {
                "image": image_repo,
                "name": "mqtt-lwt"
              }
            ]
          }
        }
      }
    }
}
