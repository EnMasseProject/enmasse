local common = import "common.jsonnet";
{
  deployment(addressSpace, image_repo)::
    {
      local certSecretName = "mqtt-lwt-internal-cert",
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "mqtt-lwt",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace,
          "enmasse.io/cert-secret": certSecretName
        },
        "name": "mqtt-lwt"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "mqtt-lwt",
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
                "name": "mqtt-lwt",
                "env": [
                  {
                    "name": "CERT_DIR",
                    "value": "/etc/enmasse-certs"
                  },
                  {
                    "name": "MESSAGING_SERVICE_NORMAL_PORT",
                    "value": "$(MESSAGING_SERVICE_PORT_AMQPS_NORMAL)"
                  },
                  {
                    "name": "MESSAGING_SERVICE_ROUTE_CONTAINER_PORT",
                    "value": "$(MESSAGING_SERVICE_PORT_AMQPS_BROKER)"
                  }
                ],
                "volumeMounts": [
                  {
                    "name": certSecretName,
                    "mountPath": "/etc/enmasse-certs",
                    "readOnly": true
                  }
                ]
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
