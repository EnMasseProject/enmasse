local version = std.extVar("VERSION");
local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("ragent", image_name),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "ragent",
          "app": "enmasse"
        },
        "name": "ragent"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "ragent"
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
                "ragent"
              ],
              "from": {
                "kind": "ImageStreamTag",
                "name": "ragent:" + version
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "ragent",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              {
                "image": "ragent",
                "name": "ragent",
                "ports": [
                  {
                    "name": "amqp",
                    "containerPort": 55672,
                    "protocol": "TCP"
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
