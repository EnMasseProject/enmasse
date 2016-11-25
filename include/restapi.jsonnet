local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("enmasse-rest", image_name),
  service::
    common.service("restapi", "restapi", "http", 8080, 8080),
  deployment::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "name": "restapi"
        },
        "name": "restapi"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "restapi"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          common.trigger("restapi", "enmasse-rest")
        ],
        "template": {
          "metadata": {
            "labels": {
              "name": "restapi",
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
