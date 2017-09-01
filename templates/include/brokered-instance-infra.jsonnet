local images = import "images.jsonnet";
{
  "apiVersion": "v1",
  "kind": "Template",
  "metadata": {
    "labels": {
      "app": "enmasse"
    },
    "name": "brokered-instance-infra"
  },

  "objects": [
    {
      "apiVersion": "v1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": "${ADDRESS_SPACE}"
        },
        "name": "broker"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "app": "enmasse",
              "role": "broker"
            },
            "annotations": {
              "addressSpace": "${ADDRESS_SPACE}"
            }
          },
          "spec": {
            "volumes": [
              {
                "name": "${MESSAGING_SECRET}",
                "secret": {
                  "secretName": "${MESSAGING_SECRET}"
                }
              }
            ],
            "containers": [
              {
                "name": "broker",
                "image": "${BROKER_IMAGE}",
                "volumeMounts": [
                  {
                    "name": "${MESSAGING_SECRET}",
                    "mountPath": "/etc/enmasse-certs",
                    "readOnly": true
                  }
                ],
                "ports": [
                  {
                    "name": "amqp",
                    "containerPort": 5672
                  }
                ],
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "amqp"
                  }
                },
                "readinessProbe": {
                  "tcpSocket": {
                    "port": "amqp"
                  }
                },
              }
            ]
          }
        }
      }
    }
  ],
  "parameters": [
    {
      "name": "MESSAGING_SECRET",
      "description": "The secret to mount for router private key and certificate",
      "required": true
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
      "name": "BROKER_IMAGE",
      "description": "Docker image to use for broker",
      "default": images.artemis
    }
  ],
}
