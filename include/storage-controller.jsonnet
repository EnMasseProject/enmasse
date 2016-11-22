local version = std.extVar("VERSION");
local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("storage-controller", image_name),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "storage-controller",
          "app": "enmasse"
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
                "name": "storage-controller:" + version
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "storage-controller",
              "app": "enmasse"
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
