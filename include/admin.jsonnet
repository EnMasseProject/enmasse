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
            "name": "address-controller",
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
        common.trigger("ragent", "ragent"),
        common.trigger("configserv", "configserv"),
        common.trigger("address-controller", "address-controller")
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
            common.container2("address-controller", "address-controller", "amqp", 55674, "http", 8080, "256Mi"),
            common.containerWithEnv("ragent", "ragent", "amqp", 55672, [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      }], "64Mi"),
            common.container("configserv", "configserv", "amqp", 5672, "256Mi"),
          ]
        }
      }
    }
  }
}
