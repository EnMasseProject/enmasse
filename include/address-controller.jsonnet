local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  common_service(name, type="ClusterIP")::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      }
    },
    "spec": {
      "ports": [
        {
          "name": "http",
          "port": 8080,
          "protocol": "TCP",
          "targetPort": "http"
        },
        {
          "name": "amqp",
          "port": 5672,
          "protocol": "TCP",
          "targetPort": "amqp"
        }
      ],
      "selector": {
        "name": "address-controller"
      },
      "type": type
    }
  },
  
  internal_service::
    self.common_service("address-controller-internal"),

  external_service::
    self.common_service("address-controller", "LoadBalancer"),

  deployment(image_repo, multiinstance, instance_idle_timeout)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "address-controller",
          "app": "enmasse"
        },
        "name": "address-controller"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "address-controller",
              "app": "enmasse"
            }
          },
          "spec": {
            "serviceAccount": "enmasse-service-account",
            "containers": [
              common.container2("address-controller", image_repo, "amqp", 5672, "http", 8080, "256Mi", [{
                "name": "MULTIINSTANCE",
                "value": multiinstance
              }, {
                "name": "INSTANCE_IDLE_TIMEOUT_SECONDS",
                "value": std.toString(instance_idle_timeout)
              }], [
                {
                  "name": "templates",
                  "mountPath": "/templates"
                }
              ])
            ],
            "volumes": [
              {
                "name": "templates",
                "configMap": {
                  "name": "enmasse-template-config"
                }
              }
            ]
          }
        }
      }
    }
}
