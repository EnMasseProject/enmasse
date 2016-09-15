{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "storage-controller"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": "latest",
            "annotations": {
              "description": "EnMasse storage controller",
              "tags": "enmasse,messaging,storage,controller",
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
          "name": "storage-controller"
        },
        "name": "storage-controller"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "storage-controller"
        },
        "strategy": {
          "type": "Rolling",
          "rollingParams": {
            "maxSurge": 0
          }
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
                "controller"
              ],
              "from": {
                "kind": "ImageStreamTag",
                "name": "storage-controller:latest"
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "storage-controller"
            }
          },
          "spec": {
            "serviceAccount": "deployer",
            "containers": [
              {
                "image": "storage-controller",
                "name": "controller",
                "ports": [
                  {
                    "name": "health",
                    "containerPort": 8080
                  }
                ],
                "livenessProbe": {
                  "httpGet": {
                    "path": "/health",
                    "port": "health"
                  }
                }
              }
            ]
          }
        }
      }
    }
}
