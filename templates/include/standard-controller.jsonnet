local common = import "common.jsonnet";
local certSecretName = "standard-controller-internal-cert";
{
  deployment(template_config)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "component": "standard-controller",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": "${ADDRESS_SPACE}",
          "io.enmasse.certSecretName" : certSecretName
        },
        "name": "standard-controller"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "component": "standard-controller",
              "app": "enmasse",
            }
          },

          "spec": {
            "serviceAccount": "${ADDRESS_SPACE_ADMIN_SA}",
            "containers": [
              {
                "image": "${STANDARD_CONTROLLER_IMAGE}",
                "name": "standard-controller",
                "env": [
                  common.env("CERT_DIR", "/etc/enmasse-certs"),
                  common.env("ADDRESS_SPACE", "${ADDRESS_SPACE}"),
                  common.env("AUTHENTICATION_SERVICE_HOST", "${AUTHENTICATION_SERVICE_HOST}"),
                  common.env("AUTHENTICATION_SERVICE_PORT", "${AUTHENTICATION_SERVICE_PORT}"),
                  common.env("AUTHENTICATION_SERVICE_CA_CERT", "${AUTHENTICATION_SERVICE_CA_CERT}"),
                  common.env("AUTHENTICATION_SERVICE_CLIENT_SECRET", "${AUTHENTICATION_SERVICE_CLIENT_SECRET}"),
                  common.env("AUTHENTICATION_SERVICE_SASL_INIT_HOST", "${AUTHENTICATION_SERVICE_SASL_INIT_HOST}"),
                  common.env("MESSAGING_SECRET", "${MESSAGING_SECRET}"),
                ] + (if template_config != "" then [ common.env("TEMPLATE_DIR", "/enmasse-templates") ] else []),

                "volumeMounts": [
                  common.volume_mount(certSecretName, "/etc/enmasse-certs", true)
                ] + (if template_config != "" then [ common.volume_mount("templates", "/enmasse-templates") ] else []),

                "resources": {
                    "requests": {
                        "memory": "128Mi",
                    },
                    "limits": {
                        "memory": "128Mi",
                    }
                },
                "ports": [
                  common.container_port("http", 8080)
                ],
               "livenessProbe": common.http_probe("http", "/health", "HTTP", 30),
                "readinessProbe": common.http_probe("http", "/health", "HTTP", 30),
              }
            ],
            "volumes": [
              common.secret_volume(certSecretName, certSecretName)
            ] + (if template_config != "" then [ common.configmap_volume("templates", template_config) ] else []),
          }
        }
      }
    }
}
