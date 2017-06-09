local router = import "router.jsonnet";
local console = import "console.jsonnet";
local common = import "common.jsonnet";
{
  service(name, instance, ports)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": name,
        "labels": {
          "app": "enmasse"
        },
        "annotations": {
          "instance": instance
        }
      },
      "spec": {
        "ports": ports,
        "selector": {
          "name": "admin"
        }
      }
    },

  services(instance)::
  [
    self.service("ragent", instance, [{"name": "amqp", "port": 5672, "targetPort": 55672}]),
    self.service("configuration", instance, [{"name": "amqp", "port": 5672}]),
    self.service("queue-scheduler", instance, [{"name": "amqp", "port": 5672, "targetPort": 55667}]),
    self.service("console", instance, [{"name": "amqp-ws", "port": 5672, "targetPort": 56720}, {"name": "http", "port": 8080}])
  ],

  deployment(use_sasldb, instance, configserv_image, ragent_image, scheduler_image, console_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "app": "enmasse",
        "name": "admin",
      },
      "annotations": {
        "instance": instance
      },
      "name": "admin"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "admin",
            "app": "enmasse",
          },
          "annotations": {
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
            console.container(use_sasldb, console_image, [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      },
                      {
                        "name": "ADDRESS_SPACE_SERVICE_HOST",
                        "value": "${ADDRESS_SPACE_SERVICE_HOST}"
                      },
                      {
                        "name": "INSTANCE",
                        "value": instance
                      }
                      ]),
            common.container("configserv", configserv_image, "amqp", 5672, "256Mi", []),
          ],
          [if use_sasldb then "volumes" ]: [router.sasldb_volume()]
        }
      }
    }
  }
}
