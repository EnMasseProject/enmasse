local configserv = import "configserv.jsonnet";
local common = import "common.jsonnet";
local images = import "images.jsonnet";
local admin = import "admin.jsonnet";
local auth_service = import "auth-service.jsonnet";
local hawkularBrokerConfig = import "hawkular-broker-config.jsonnet";

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
        },
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
          "name": "amqps",
          "port": 5671,
          "targetPort": "amqps-internal"
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
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "io.enmasse.certSecretName" : "broker-internal-cert"
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
            "addressSpace": "${ADDRESS_SPACE}"
          }
        },
        "spec": {
          "volumes": [
            common.persistent_volume("data", "broker-data"),
            common.configmap_volume("hawkular-openshift-agent", "hawkular-broker-config"),
            common.secret_volume("broker-internal-cert", "broker-internal-cert"),
          ],
          "containers": [
            {
              "name": "broker",
              "image": "${BROKER_IMAGE}",
              "env": [
                common.env("ADDRESS_SPACE_TYPE", "brokered"),
                common.env("CERT_DIR", "/etc/enmasse-certs"),
              ],
              "volumeMounts": [
                common.volume_mount("data", "/var/run/artemis"),
                common.volume_mount("broker-internal-cert", "/etc/enmasse-certs", true)
              ],
              "ports": [
                common.container_port("amqp", 5672),
                common.container_port("amqps", 5671),
                common.container_port("amqps-internal", 5673),
                common.container_port("jolokia", 8161)
              ],
              "livenessProbe": common.tcp_probe("amqp", 120),
              "readinessProbe": common.tcp_probe("amqp", 0),
            }
          ]
        }
      }
    }
  },

  console_deployment(name)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": "${ADDRESS_SPACE}",
        "io.enmasse.certSecretName" : "console-internal-cert"
      }
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "app": "enmasse",
            "role": "console",
            "name": name
          },
          "annotations": {
            "addressSpace": "${ADDRESS_SPACE}"
          }
        },
        "spec": {
          "volumes": [
            common.secret_volume("authservice-ca", "authservice-ca"),
            common.secret_volume("console-internal-cert", "console-internal-cert")
          ],
          "containers": [
            {
              "name": "console",
              "image": "${CONSOLE_IMAGE}",
              "env": [
                common.env("ADDRESS_SPACE", "${ADDRESS_SPACE}"),
                common.env("ADDRESS_SPACE_TYPE", "brokered"),
                common.env("ADDRESS_SPACE_SERVICE_HOST", "${ADDRESS_SPACE_SERVICE_HOST}"),
                common.env("CERT_DIR", "/etc/enmasse-certs"),
              ] + auth_service.envVars,
              "volumeMounts": [
                common.volume_mount("authservice-ca", "/opt/console/authservice-ca", true),
                common.volume_mount("console-internal-cert", "/etc/enmasse-certs", true)
              ],
              "ports": [
                common.container_port("http", 8080)
              ],
              "livenessProbe": {
                "httpGet": {
                  "path": "/probe",
                  "port": "http"
                }
              },
              "readinessProbe": {
                "httpGet": {
                  "path": "/probe",
                  "port": "http"
                }
              }
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
      "name": "console"
    },
    "spec": {
      "ports": [
        {
          "name": "http",
          "port": 8080,
          "targetPort": "http"
        }
      ],
      "selector": {
        "role": "console"
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
      common.ca_secret("address-controller-ca", "${ADDRESS_CONTROLLER_CA_CERT}"),
      admin.password_secret("address-space-credentials", "${ADDRESS_SPACE_PASSWORD}"),
      hawkularBrokerConfig,
      configserv.deployment("${ADDRESS_SPACE}", "${CONFIGSERV_IMAGE}"),
      configserv.service("${ADDRESS_SPACE}"),
      me.pvc("broker-data"),
      me.broker_deployment("broker"),
      me.broker_service,
      me.messaging_service,
      me.console_deployment("console"),
      me.console_service
    ],
    "parameters": [
      {
        "name": "ADDRESS_SPACE_SERVICE_HOST",
        "description": "Hostname where API server can be reached",
        "value": ""
      },
      {
        "name": "CONFIGSERV_IMAGE",
        "description": "The image to use for the configuration service",
        "value": images.configserv
      },
      {
        "name": "BROKER_IMAGE",
        "description": "The docker image to use for the message broker",
        "value": images.artemis
      },
      {
        "name": "CONSOLE_IMAGE",
        "description": "The docker image to use for the console server",
        "value": images.console
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
        "name": "ADDRESS_CONTROLLER_CA_CERT",
        "description": "The CA cert to use for validating address controller identity"
      },
      {
        "name": "ADDRESS_SPACE_PASSWORD",
        "description": "Password for authenticating against address controller"
      },
    ],
  }
}
