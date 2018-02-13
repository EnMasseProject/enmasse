local common = import "common.jsonnet";
{
  service(name, addressSpace, ports, default_port="")::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": name,
        "labels": {
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace,
          [if default_port != "" then "io.enmasse.endpointPort"]: default_port
        }
      },
      "spec": {
        "ports": ports,
        "selector": {
          "name": "admin"
        }
      }
    },

  services(addressSpace)::
  [
    self.service("ragent", addressSpace, [{"name": "amqp", "port": 5671, "targetPort": 55671}]),
    self.service("queue-scheduler", addressSpace, [{"name": "amqp", "port": 5672, "targetPort": 55671}]),
    self.service("console", addressSpace, [{"name": "https", "port": 8081, "targetPort": 8080}], "https")
  ],


  deployment(auth_service_ca_secret, address_controller_ca_secret, template_config)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "app": "enmasse",
        "name": "admin",
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "io.enmasse.certSecretName": "admin-internal-cert"
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
            "addressSpace": "${ADDRESS_SPACE}"
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
                common.env("AUTHENTICATION_SERVICE_CA_SECRET", auth_service_ca_secret),
                common.env("AUTHENTICATION_SERVICE_CLIENT_SECRET", "${AUTHENTICATION_SERVICE_CLIENT_SECRET}"),
                common.env("JAVA_OPTS", "-verbose:gc"),
                common.env("AUTHENTICATION_SERVICE_SASL_INIT_HOST", "${AUTHENTICATION_SERVICE_SASL_INIT_HOST}"),
                common.env("MESSAGING_SECRET", "${MESSAGING_SECRET}"),
              ] + (if template_config != "" then [ common.env("TEMPLATE_DIR", "/enmasse-templates") ] else []),
              "resources": common.memory_resources("512Mi", "512Mi"),
              "ports": [
                common.container_port("http", 8889)
              ],
              "livenessProbe": common.http_probe("http", "/health", "HTTP", 30),
              "readinessProbe": common.http_probe("http", "/health", "HTTP", 30),
              "volumeMounts": [
                common.volume_mount("admin-internal-cert", "/etc/enmasse-certs", true)
              ] + (if template_config != "" then [ common.volume_mount("templates", "/enmasse-templates") ] else []),
            },
            {
              "image": "${AGENT_IMAGE}",
              "name": "agent",
              "env": [
                common.env("CERT_DIR", "/etc/enmasse-certs"),
                common.env("AUTHENTICATION_SERVICE_HOST", "${AUTHENTICATION_SERVICE_HOST}"),
                common.env("AUTHENTICATION_SERVICE_PORT", "${AUTHENTICATION_SERVICE_PORT}"),
                common.env("AUTHENTICATION_SERVICE_CA_SECRET", auth_service_ca_secret),
                common.env("AUTHENTICATION_SERVICE_CLIENT_SECRET", "${AUTHENTICATION_SERVICE_CLIENT_SECRET}"),
                common.env("AUTHENTICATION_SERVICE_SASL_INIT_HOST", "${AUTHENTICATION_SERVICE_SASL_INIT_HOST}"),
                common.env("ADDRESS_SPACE", "${ADDRESS_SPACE}"),
                common.env("ADDRESS_SPACE_SERVICE_HOST", "${ADDRESS_SPACE_SERVICE_HOST}"),
                common.env("ADDRESS_CONTROLLER_CA", "/opt/agent/address-controller-ca/tls.crt"),
                common.env("MESSAGING_CERT", "/opt/agent/messaging-cert/tls.crt")
              ],
              "resources": common.memory_resources("128Mi", "128Mi"),
              "ports": [
                common.container_port("https", 8080),
                common.container_port("amqp-ws", 56720)
              ],
              "livenessProbe": common.http_probe("https", "/probe", "HTTPS"),
              "readinessProbe": common.http_probe("https", "/probe", "HTTPS"),
              "volumeMounts": [
                common.volume_mount("console-secret", "/etc/console-certs", true),
                common.volume_mount("authservice-ca", "/opt/agent/authservice-ca", true),
                common.volume_mount("admin-internal-cert", "/etc/enmasse-certs", true),
                common.volume_mount("address-controller-ca", "/opt/agent/address-controller-ca", true),
                common.volume_mount("messaging-cert", "/opt/agent/messaging-cert", true)
              ]
            },
          ],
          "volumes": [
            common.secret_volume("console-secret", "${CONSOLE_SECRET}"),
            common.secret_volume("authservice-ca", auth_service_ca_secret),
            common.secret_volume("address-controller-ca", address_controller_ca_secret),
            common.secret_volume("admin-internal-cert", "admin-internal-cert"),
            common.secret_volume("messaging-cert", "${MESSAGING_SECRET}")
          ] + (if template_config != "" then [ common.configmap_volume("templates", template_config) ] else []),
        }
      }
    }
  }
}
