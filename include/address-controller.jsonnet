local common = import "common.jsonnet";
{
  common_service(name, type)::
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
    self.common_service("address-controller", "ClusterIP"),

  external_service::
    self.common_service("address-controller-external", "LoadBalancer"),

  deployment(image_repo, multiinstance, secret_name, template_config, instance_idle_timeout)::
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

          local template_mount = [{
              "name": "templates",
              "mountPath": "/enmasse-templates"
          }],

          local ssl_certs = [{
            "name": "ssl-certs",
            "mountPath": "/ssl-certs",
            "readOnly": true
          }],

          local mounts = if template_config != ""
            then template_mount
            else [],

          "spec": {
            "serviceAccount": "enmasse-service-account",
            "containers": [
              common.container2("address-controller", image_repo, "amqp", 5672, "http", 8080, "256Mi", [{
                "name": "MULTIINSTANCE",
                "value": multiinstance
              }, {
                "name": "INSTANCE_IDLE_TIMEOUT_SECONDS",
                "value": std.toString(instance_idle_timeout)
              }], mounts)

            ],
            local template_volume = [{
                "name": "templates",
                "configMap": {
                  "name": template_config
                }
            }],

            local secret_volume = [{
                "name": "ssl-certs",
                "secret": {
                  "secretName": secret_name
                }
            }],

            "volumes": if template_config != ""
              then template_volume + secret_volume
              else secret_volume
          }
        }
      }
    }
}
