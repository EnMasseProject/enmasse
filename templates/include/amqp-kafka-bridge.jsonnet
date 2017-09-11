local common = import "common.jsonnet";
{
  deployment(addressSpace, image_repo)::
    {
      local certSecretName = "amqp-kafka-bridge-internal-cert",
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "amqp-kafka-bridge",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace,
          "io.enmasse.certSecretName": certSecretName
        },
        "name": "amqp-kafka-bridge"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "capability": "bridge",
              "name": "amqp-kafka-bridge",
              "app": "enmasse"
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [
              {
                "image": image_repo,
                "name": "amqp-kafka-bridge",
                "env": [
                  {
                    "name": "AMQP_CERT_DIR",
                    "value": "/etc/enmasse-certs"
                  },
                  {
                    "name": "KAFKA_BOOTSTRAP_SERVERS",
                    "value": "${KAFKA_BOOTSTRAP_SERVERS}"
                  }
                ],
                "resources": {
                    "requests": {
                        "memory": "512Mi"
                    },
                    "limits": {
                        "memory": "512Mi"
                    }
                },
                "volumeMounts": [
                  {
                    "name": certSecretName,
                    "mountPath": "/etc/enmasse-certs",
                    "readOnly": true
                  }
                ],
                "ports": [
                  {
                    "name": "health",
                    "containerPort": "8080"
                  },
                  {
                    "name": "ready",
                    "containerPort": "8080"
                  }
                ],
                "livenessProbe": {
                  "httpGet": {
                    "path": "/health",
                    "port": "health"
                  }
                },
                "readinessProbe": {
                  "httpGet": {
                    "path": "/ready",
                    "port": "ready"
                  }
                },
              }
            ],
            "volumes": [
              {
                "name": certSecretName,
                "secret": {
                  "secretName": certSecretName
                }
              }
             ]
          }
        }
      }
    }
}
