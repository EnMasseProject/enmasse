local version = std.extVar("VERSION");
{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "configmap-bridge"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": version,
            "annotations": {
              "description": "ConfigMap AMQP Bridge",
              "tags": "enmasse,messaging,configmap,amqp",
              "version": "1.0"
            }
          }
        ],
        "importPolicy": {
          "scheduled": true
        }
      }
    },
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
