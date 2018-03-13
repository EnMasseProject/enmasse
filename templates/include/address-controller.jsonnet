local common = import "common.jsonnet";
{
  common_service(name, type, annotations)::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      },
      "annotations": annotations
    },
    "spec": {
      "ports": [
        {
          "name": "https",
          "port": 443,
          "protocol": "TCP",
          "targetPort": "https"
        }
      ],
      "selector": {
        "name": "address-controller"
      },
      "type": type
    }
  },
  
  internal_service::
    self.common_service("address-controller", "ClusterIP", {}),

  external_service::
    self.common_service("restapi-external", "LoadBalancer", {}),

  deployment(image, template_config, cert_secret, environment, enable_rbac, enable_event_logger, address_controller_sa, address_space_admin_sa, wildcard_endpoint_cert_secret, resync_interval, check_interval)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "address-controller",
          "app": "enmasse",
          "environment": environment
        },
        "name": "address-controller"
      },
      "spec": {
        "replicas": 1,
        "strategy": {
          "type": "Recreate"
        },
        "template": {
          "metadata": {
            "labels": {
              "name": "address-controller",
              "app": "enmasse",
              "environment": environment
            }
          },

          "spec": {
            "serviceAccount": address_controller_sa,
            "containers": [
              {
                "image": image,
                "name": "address-controller",
                "env": [
                  common.env("ENABLE_RBAC", enable_rbac),
                  common.env("ENABLE_EVENT_LOGGER", enable_event_logger),
                  common.env("ENVIRONMENT", environment),
                  common.env("ADDRESS_CONTROLLER_SA", address_controller_sa),
                  common.env("ADDRESS_SPACE_ADMIN_SA", address_space_admin_sa),
                  common.env("WILDCARD_ENDPOINT_CERT_SECRET", wildcard_endpoint_cert_secret),
                  common.env("RESYNC_INTERVAL", resync_interval),
                  common.env("CHECK_INTERVAL", check_interval)
                ],
                "volumeMounts": [
                  common.volume_mount("address-controller-cert", "/address-controller-cert", true),
                ] + if template_config != "" then [ common.volume_mount("templates", "/enmasse-templates") ] else [],
                "resources": {
                    "requests": {
                        "memory": "512Mi",
                    },
                    "limits": {
                        "memory": "512Mi",
                    }
                },
                "ports": [
                  common.container_port("https", 8081),
                  common.container_port("http", 8080)
                ],
               "livenessProbe": common.http_probe("https", "/apis/enmasse.io/v1/health", "HTTPS", 30),
                "readinessProbe": common.http_probe("https", "/apis/enmasse.io/v1/health", "HTTPS", 30),
              }
            ],
            "volumes": [
              common.secret_volume("address-controller-cert", cert_secret)
            ] + if template_config != "" then [ common.configmap_volume("templates", template_config) ] else [],
          }
        }
      }
    }
}
