local version = std.extVar("VERSION");
local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("configmap-bridge", image_name),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "configmap-bridge"
        },
        "name": "configmap-bridge"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "configmap-bridge"
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
                "name": "configmap-bridge:" + version
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "configmap-bridge"
            }
          },
          "spec": {
            "containers": [
              {
                "image": "configmap-bridge",
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
