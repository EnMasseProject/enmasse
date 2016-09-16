{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "subserv"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": "latest",
            "annotations": {
              "description": "Subscription service",
              "tags": "enmasse,messaging,amqp,subscription,topic",
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
              "name": "subserv:latest"
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
