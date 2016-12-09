local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  services::
  [
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "admin",
        "labels": {
          "app": "enmasse"
        }
      },
      "spec": {
        "ports": [
          {
            "name": "ragent",
            "port": 55672
          },
          {
            "name": "restapi",
            "port": 8080
          },
          {
            "name": "configuration",
            "port": 5672
          },
          {
            "name": "storage-controller",
            "port": 55674
          }
        ],
        "selector": {
          "name": "admin"
        }
      }
    }
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
                "tcpSocket": {
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
