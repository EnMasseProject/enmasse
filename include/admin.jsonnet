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
          },
          {
            "name": "queue-scheduler",
            "port": 55667
          }
        ],
        "selector": {
          "name": "admin"
        }
      }
    }
  ],
  deployment(controller_image, configserv_image, ragent_image, scheduler_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "app": "enmasse",
        "name": "admin"
      },
      "name": "admin"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "admin",
            "app": "enmasse"
          }
        },
        "spec": {
          "serviceAccount": "enmasse-service-account",
          "containers": [
            common.container2("address-controller", controller_image, "amqp", 55674, "http", 8080, "256Mi"),
            common.containerWithEnv("ragent", ragent_image, "amqp", 55672, [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      }], "64Mi"),
            common.containerWithEnv("queue-scheduler", scheduler_image, "amqp", 55667, [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      }], "128Mi"),
            common.container("configserv", configserv_image, "amqp", 5672, "256Mi"),
          ]
        }
      }
    }
  }
}
