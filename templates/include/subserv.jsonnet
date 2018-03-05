local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "subscription", "subserv", "amqp", 5672, 5672),
  deployment(addressSpace, container_image)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "name": "subserv",
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": addressSpace,
        "enmasse.io/cert-secret": "subserv-internal-cert"
      },
      "name": "subserv"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "subserv",
            "app": "enmasse"
          },
          "annotations": {
            "addressSpace": addressSpace
          }
        },
        "spec": {
          "containers": [
            {
              "image": container_image,
              "command": [std.extVar("NODE_CMD"), "/opt/app-root/src/bin/subserv.js"],
              "name": "subserv",
              "resources": {
                  "requests": {
                      "memory": "128Mi"
                  },
                  "limits": {
                      "memory": "128Mi"
                  }
              },
              "ports": [
                {
                  "name": "amqp",
                  "containerPort": 5672,
                  "protocol": "TCP"
                }
              ],
              "livenessProbe": {
                "tcpSocket": {
                  "port": "amqp"
                },
                "initialDelaySeconds": 60
              },
              "volumeMounts": [
                {
                  "name": "subserv-internal-cert",
                  "mountPath": "/etc/enmasse-certs",
                  "readOnly": true
                }
              ]
            },
          ],
          "volumes": [
            {
                "name": "subserv-internal-cert",
                "secret": {
                    "secretName": "subserv-internal-cert"
                }
            }
          ]
        }
      }
    }
  }
}
