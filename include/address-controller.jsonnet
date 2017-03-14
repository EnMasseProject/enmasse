local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "address-controller",
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
      }
    }
  },
  deployment(secure, image_repo, multitenant)::
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
                "name": "MULTITENANT",
                "value": multitenant
              }, {
                "name": "TLS",
                "value": secure
              }])
            ]
          }
        }
      }
    }
}
