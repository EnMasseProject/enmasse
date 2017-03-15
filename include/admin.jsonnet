local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  services(instance)::
  [
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "admin",
        "labels": {
          "app": "enmasse",
          "instance": instance
        }
      },
      "spec": {
        "ports": [
          {
            "name": "ragent",
            "port": 55672
          },
          {
            "name": "configuration",
            "port": 5672
          },
          {
            "name": "queue-scheduler",
            "port": 55667
          }
        ],
        "selector": {
          "name": "admin",
          "instance": instance
        }
      }
    }
  ],
  deployment(instance, configserv_image, ragent_image, scheduler_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "app": "enmasse",
        "name": "admin",
        "instance": instance
      },
      "name": instance + "-admin"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "admin",
            "app": "enmasse",
            "instance": instance
          }
        },
        "spec": {
          "containers": [
            common.container("ragent", ragent_image, "amqp", 55672, "64Mi", [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      }]),
            common.container("queue-scheduler", scheduler_image, "amqp", 55667, "128Mi", [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      }]),
            common.container("configserv", configserv_image, "amqp", 5672, "256Mi", []),
          ]
        }
      }
    }
  }
}
