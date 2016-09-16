{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "ragent"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": "latest",
            "annotations": {
              "description": "EnMasse router agent",
              "tags": "enmasse,messaging,ragent,config,amqp",
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
          "name": "ragent"
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
                "name": "ragent:latest"
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "ragent"
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
