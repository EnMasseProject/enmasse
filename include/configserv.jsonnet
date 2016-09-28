local version = std.extVar("VERSION");
local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("configserv", image_name),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
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
