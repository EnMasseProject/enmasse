local version = std.extVar("VERSION");
local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("subserv", image_name),
  deployment::
  {
    "apiVersion": "v1",
    "kind": "DeploymentConfig",
    "metadata": {
      "labels": {
        "name": "subserv"
      },
      "name": "subserv"
    },
    "spec": {
      "replicas": 1,
      "selector": {
        "name": "subserv"
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
              "subserv"
            ],
            "from": {
              "kind": "ImageStreamTag",
              // TODO: Update to use version when travis is setup for subserv
              "name": "subserv:latest",
            }
          }
        }
      ],
      "template": {
        "metadata": {
          "labels": {
            "name": "subserv"
          }
        },
        "spec": {
          "containers": [
            {
              "image": "subserv",
              "name": "subserv",
              "ports": [
                {
                  "containerPort": 5672,
                  "name": "amqp",
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
