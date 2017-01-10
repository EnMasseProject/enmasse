local version = std.extVar("VERSION");
local mqtt = import "mqtt.jsonnet";
local common = import "common.jsonnet";
{
  deployment(secure)::
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
            "containers": [ mqtt.container(secure) ],
            [if secure then "volumes" ]: [
              mqtt.secret_volume()
            ]
          }
        }
      }
    }
}
