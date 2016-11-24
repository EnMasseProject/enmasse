local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  services::
  [
    common.service("ragent", "admin", 55672, 55672),
    common.service("restapi", "admin", 8080, 8080),
    common.service("configuration", "admin", 5672, 5672),
    common.service("storage-controller", "admin", 5672, 55674)
  ],
  deployment::
  {
    "apiVersion": "v1",
    "kind": "DeploymentConfig",
    "metadata": {
      "labels": {
        "app": "enmasse",
        "name": "admin"
      },
      "name": "admin"
    },
    "spec": {
      "replicas": 1,
      "selector": {
        "name": "admin"
      },
      "triggers": [
        {
          "type": "ConfigChange"
        },
        common.trigger("restapi", "enmasse-rest"),
        common.trigger("ragent", "ragent"),
        common.trigger("configserv", "configserv"),
        common.trigger("storage-controller", "storage-controller")
      ],
      "template": {
        "metadata": {
          "labels": {
            "name": "admin",
            "app": "enmasse"
          }
        },
        "spec": {
          "serviceAccount": "deployer",
          "containers": [
            common.container("storage-controller", "storage-controller", "amqp", 55674),
            common.container("ragent", "ragent", "amqp", 55672),
            common.container("configserv", "configserv", "amqp", 5672),
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
