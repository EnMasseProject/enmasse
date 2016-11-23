local version = std.extVar("VERSION");
local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("enmasse-rest", image_name),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "component": "restapi"
        },
        "name": "restapi"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "component": "restapi"
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
                "restapi"
              ],
              "from": {
                "kind": "ImageStreamTag",
                "name": "enmasse-rest:" + version
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "component": "restapi",
              "app": "enmasse"
            }
          },
          "spec": {
            "serviceAccount": "deployer",
            "containers": [
              {
                "image": "enmasse-rest",
                "name": "restapi",
                "ports": [
                  {
                    "name": "http",
                    "containerPort": 8080
                  }
                ],
                "livenessProbe": {
                  "httpGet": {
                    "path": "/v1/enmasse/addresses",
                    "port": "http"
                  }
                }
              }
            ]
          }
        }
      }
    }
}
