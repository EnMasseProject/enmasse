{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "configserv"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": "${ENMASSE_VERSION}",
            "annotations": {
              "description": "Configuration Service",
              "tags": "enmasse,messaging,config,configserv,amqp",
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
                "name": "configserv:${ENMASSE_VERSION}"
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
