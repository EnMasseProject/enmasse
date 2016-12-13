local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("mqtt-frontend", image_name),
  service::
    common.service("mqtt-frontend", "mqtt-frontend", "mqtt", 1883, 1883),
  deployment::
  {
    "apiVersion": "v1",
    "kind": "DeploymentConfig",
    "metadata": {
      "labels": {
        "name": "mqtt-frontend",
        "app": "enmasse"
      },
      "name": "mqtt-frontend"
    },
    "spec": {
      "replicas": 1,
      "selector": {
        "name": "mqtt-frontend"
      },
      "triggers": [
        {
          "type": "ConfigChange"
        },
        common.trigger("mqtt-frontend", "mqtt-frontend")
      ],
      "template": {
        "metadata": {
          "labels": {
            "name": "mqtt-frontend",
            "app": "enmasse"
          }
        },
        "spec": {
          "containers": [
            common.container("mqtt-frontend", "mqtt-frontend", "mqtt", 1883)
          ]
        }
      }
    }
  }
}
