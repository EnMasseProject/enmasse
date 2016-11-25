local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("configserv", image_name),
  service::
    common.service("configuration", "configserv", "amqp", 5672, 5672),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "name": "configserv"
        },
        "name": "configserv"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "configserv"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          {
            "type": "ImageChange",
            "imageChangeParams": {
              "automatic": true,
              "containerNames": [
                "bridge"
              ],
              "from": {
                "kind": "ImageStreamTag",
                "name": "configserv:" + version
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "app": "enmasse",
              "name": "configserv"
            }
          },
          "spec": {
            "containers": [
              {
                "image": "configserv",
                "name": "bridge",
                "ports": [
                  {
                    "name": "amqp",
                    "containerPort": 5672
                  }
                ],
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "amqp"
                  }
                }
              }
            ]
          }
        }
      }
    }
}
