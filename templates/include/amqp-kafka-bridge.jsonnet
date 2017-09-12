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
              common.clientContainer("amqp-kafka-bridge", image_repo, "512Mi", [{
                          "name": "AMQP_CERT_DIR",
                          "value": "/etc/enmasse-certs"
                        }, {
                          "name": "KAFKA_BOOTSTRAP_SERVERS",
                          "value": "${KAFKA_BOOTSTRAP_SERVERS}"
                        }], [{
                          "name": certSecretName,
                          "mountPath": "/etc/enmasse-certs",
                          "readOnly": true
                        }],
                        true, true),
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
