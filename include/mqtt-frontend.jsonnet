local version = std.extVar("VERSION");
local mqtt = import "mqtt.jsonnet";
local common = import "common.jsonnet";
{
  deployment(secure)::
    local container = common.trigger("mqtt-frontend", "mqtt-frontend");
    local secureContainer = common.trigger("mqtt-frontend-tls", "mqtt-frontend");
    local triggerConfigChange = {
      "type": "ConfigChange"
    };
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
        "triggers": if secure
          then [triggerConfigChange, container, secureContainer]
          else [triggerConfigChange, container],
        "template": {
          "metadata": {
            "labels": {
              "name": "mqtt-frontend",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers":
            if secure
            then [ mqtt.container(true), mqtt.container(false) ]
            else [ mqtt.container(false) ],
            [if secure then "volumes" ]: [
              mqtt.secret_volume()
            ]
          }
        }
      }
    }
}
