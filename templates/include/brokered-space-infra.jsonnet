local common = import "common.jsonnet";
local roles = import "roles.jsonnet";
local images = import "images.jsonnet";
local admin = import "admin.jsonnet";
local auth_service = import "auth-service.jsonnet";
local prometheus = import "prometheus.jsonnet";

{

  local me = self,

  pvc(name)::
  {
    "apiVersion": "v1",
    "kind": "PersistentVolumeClaim",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}"
      }
    },
    "spec": {
      "accessModes": [
        "ReadWriteOnce"
      ],
      "resources": {
        "requests": {
          "storage": "${STORAGE_CAPACITY}"
        }
      }
    }
  },

  messaging_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "io.enmasse.endpointPort": "amqps"
      },
      "name": "messaging"
    },
    "spec": {
      "ports": [
        {
          "name": "amqp",
          "port": 5672,
          "targetPort": "amqp"
        },
        {
          "name": "amqps",
          "port": 5671,
          "targetPort": "amqps"
        }
      ],
      "selector": {
        "role": "broker"
      }
    }
  },

  broker_console_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "io.enmasse.endpointPort": "console"
      },
      "name": "brokerconsole"
    },
    "spec": {
      "ports": [
        {
          "name": "console",
          "port": 8161,
          "targetPort": "jolokia"
        }
      ],
      "selector": {
        "role": "broker"
      }
    }
  },

  broker_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "broker"
    },
    "spec": {
      "ports": [
        {
          "name": "amqps-normal",
          "port": 55671,
          "targetPort": "amqps-normal"
        }
      ],
      "selector": {
        "role": "broker"
      }
    }
  },

  broker_deployment(name)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse",
        "role": "broker"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "enmasse.io/cert-secret" : "broker-internal-cert"
      }
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "app": "enmasse",
            "role": "broker",
            "name": name
          },
          "annotations": {
            "addressSpace": "${ADDRESS_SPACE}",
            "prometheus.io/scrape": "true",
            "prometheus.io/path": "/metrics",
            "prometheus.io/port": "8080"
          }
        },
        "spec": {
          "volumes": [
            common.persistent_volume("data", "broker-data"),
            common.configmap_volume("broker-prometheus-config", "broker-prometheus-config"),
            common.secret_volume("broker-internal-cert", "broker-internal-cert"),
            common.secret_volume("authservice-ca", "authservice-ca"),
            common.secret_volume("external-cert", "${MESSAGING_SECRET}"),
          ],
          "containers": [
            {
              "name": "broker",
              "image": "${BROKER_IMAGE}",
              "env": [
                common.env("ADDRESS_SPACE_TYPE", "brokered"),
                common.env("CERT_DIR", "/etc/enmasse-certs"),
              ] + auth_service.envVars,
              "volumeMounts": [
                common.volume_mount("data", "/var/run/artemis"),
                common.volume_mount("broker-internal-cert", "/etc/enmasse-certs", true),
                common.volume_mount("external-cert", "/etc/external-certs", true),
                common.volume_mount("authservice-ca", "/etc/authservice-ca", true),
                common.volume_mount("broker-prometheus-config", "/etc/prometheus-config", true)
              ],
              "ports": [
                common.container_port("amqp", 5672),
                common.container_port("amqps", 5671),
                common.container_port("amqps-normal", 55671),
                common.container_port("jolokia", 8161),
                common.container_port("artemismetrics", 8080)
              ],
              "livenessProbe": common.exec_probe(["sh", "-c", "$ARTEMIS_HOME/bin/probe.sh"], 120),
              "readinessProbe": common.exec_probe(["sh", "-c", "$ARTEMIS_HOME/bin/probe.sh"], 10),
            }
          ]
        }
      }
    }
  },

  agent_deployment(name)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse",
        "role": "agent"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "enmasse.io/cert-secret" : "agent-internal-cert"
      }
    },
    "spec": {
      "replicas": 1,
      "strategy": {
        "type": "Recreate"
      },
      "template": {
        "metadata": {
          "labels": {
            "app": "enmasse",
            "role": "agent",
            "name": name
          },
          "annotations": {
            "addressSpace": "${ADDRESS_SPACE}"
          }
        },
        "spec": {
          "serviceAccount": "${ADDRESS_SPACE_ADMIN_SA}",
          "volumes": [
            common.secret_volume("authservice-ca", "authservice-ca"),
            common.secret_volume("console-external-cert", "${CONSOLE_SECRET}"),
            common.secret_volume("agent-internal-cert", "agent-internal-cert"),
            common.secret_volume("messaging-cert", "external-certs-messaging")
          ],
          "containers": [
            {
              "name": "agent",
              "image": "${AGENT_IMAGE}",
              "env": [
                common.env("ADDRESS_SPACE", "${ADDRESS_SPACE}"),
                common.env("ENABLE_EVENT_LOGGER", "${ENABLE_EVENT_LOGGER}"),
                common.env("ADDRESS_SPACE_TYPE", "brokered"),
                common.env("CERT_DIR", "/etc/enmasse-certs"),
                common.env("CONSOLE_CERT_DIR", "/etc/console-certs"),
                common.env("MESSAGING_CERT", "/opt/agent/messaging-cert/tls.crt"),
              ] + auth_service.envVars,
              "volumeMounts": [
                common.volume_mount("authservice-ca", "/opt/agent/authservice-ca", true),
                common.volume_mount("console-external-cert", "/etc/console-certs", true),
                common.volume_mount("agent-internal-cert", "/etc/enmasse-certs", true),
                common.volume_mount("messaging-cert", "/opt/agent/messaging-cert", true)
              ],
              "ports": [
                common.container_port("https", 8080)
              ],
              "livenessProbe": common.http_probe("https", "/probe", "HTTPS"),
              "readinessProbe": common.http_probe("https", "/probe", "HTTPS")
            }
          ]
        }
      }
    }
  },

  console_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "io.enmasse.endpointPort": "https"
      },
      "name": "console"
    },
    "spec": {
      "ports": [
        {
          "name": "https",
          "port": 8081,
          "targetPort": 8080
        }
      ],
      "selector": {
        "role": "agent"
      }
    }
  },

  template::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "brokered-space-infra"
    },
    "objects": [
      common.ca_secret("authservice-ca", "${AUTHENTICATION_SERVICE_CA_CERT}"),
      prometheus.brokered_broker_config("broker-prometheus-config"),
      me.pvc("broker-data"),
      me.broker_deployment("broker"),
      me.broker_service,
      me.messaging_service,
      me.agent_deployment("agent"),
      me.console_service
    ],
    "parameters": [
      {
        "name": "BROKER_IMAGE",
        "description": "The container image to use for the message broker",
        "value": images.artemis
      },
      {
        "name": "AGENT_IMAGE",
        "description": "The container image to use for the enmasse agent",
        "value": images.agent
      },
      {
        "name": "STORAGE_CAPACITY",
        "description": "Storage capacity required for volume claims",
        "value": "2Gi"
      },
      {
        "name": "ADDRESS_SPACE",
        "description": "The address space this infrastructure is deployed for",
        "required": true
      },
      {
        "name": "MESSAGING_SECRET",
        "description": "Certificate to be used for public messaging service",
        "required": true
      },
      {
        "name": "CONSOLE_SECRET",
        "description": "Certificate to be used for public console service",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_HOST",
        "description": "The hostname of the authentication service used by this address space",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_PORT",
        "description": "The port of the authentication service used by this address space",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_CA_CERT",
        "description": "The CA cert to use for validating authentication service cert",
        "required": true
      },
      {
        "name": "AUTHENTICATION_SERVICE_CLIENT_SECRET",
        "description": "The client cert to use as identity against authentication service",
      },
      {
        "name": "AUTHENTICATION_SERVICE_SASL_INIT_HOST",
        "description": "The hostname to use in sasl init",
      },
      {
        "name": "ENABLE_EVENT_LOGGER",
        "description": "Enable logging of kubernetes events",
        "value": "false"
      },
      {
        "name": "ADDRESS_SPACE_ADMIN_SA",
        "description": "The service account with address space admin privileges",
        "value": "address-space-admin"
      }
    ],
  }
}
