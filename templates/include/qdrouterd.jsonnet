local images = import "images.jsonnet";
{
  service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "messaging",
      "annotations": {
        "io.enmasse.endpointPort": "amqps",
        "enmasse.io/service-port.amqps": 5671,
        "enmasse.io/service-port.amqp": 5672
      }
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
        {
          "name": "amqps-normal",
          "port": 55671,
          "targetPort": "amqps-normal"
        },
        {
          "name": "amqps-broker",
          "port": 56671,
          "targetPort": "amqps-broker"
        },
        {
          "name": "amqps-router",
          "port": 55672,
          "targetPort": "amqps-router"
        },
      ],
      "selector": {
        "component": "router"
      }
    }
  },

  deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "component": "router",
        "app": "enmasse"
      },
      "annotations": {
        "enmasse.io/cert-secret" : "router-internal-cert",
        "enmasse.io/cert-cn" : "router",
        "prometheus.io/scrape": "true",
        "prometheus.io/path": "/metrics",
        "prometheus.io/port": "8080"
      },
      "name": "qdrouterd"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "capability": "router",
            "component": "router",
            "app": "enmasse"
          }
        },
        "spec": {
          "containers": [
            {
              "name": "router",
              "image": images.router,
              "env": [
                {
                  "name": "LINK_CAPACITY",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-config",
                      "key": "routerLinkCapacity",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "ALLOW_DURABLE_SUBSCRIPTIONS",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-config",
                      "key": "allowDurableSubscriptions",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "AUTHENTICATION_SERVICE_HOST",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-config",
                      "key": "authenticationServiceHost"
                    }
                  }
                },
                {
                  "name": "AUTHENTICATION_SERVICE_PORT",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-config",
                      "key": "authenticationServicePort"
                    }
                  }
                },
                {
                  "name": "AUTHENTICATION_SERVICE_CLIENT_SECRET",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-config",
                      "key": "authenticationServiceClientSecret",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "AUTHENTICATION_SERVICE_SASL_INIT_HOST",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-config",
                      "key": "authenticationServiceSaslInitHost",
                      "optional": true
                    }
                  }
                }
              ],
              "ports": [
                {
                  "name": "amqp",
                  "containerPort": 5672
                },
                {
                  "name": "amqps",
                  "containerPort": 5671
                },
                {
                  "name": "amqps-router",
                  "containerPort": 5672
                },
                {
                  "name": "amqps-normal",
                  "containerPort": 55671
                },
                {
                  "name": "amqps-broker",
                  "containerPort": 56671
                },
              ],
              "livenessProbe": {
                "exec": {
                  "command": [
                    "qdmanage",
                    "QUERY",
                    "--type=listener"
                  ]
                }
              },
              "volumeMounts": [
                {
                  "name": "ssl-certs",
                  "mountPath": "/etc/qpid-dispatch/ssl",
                  "readOnly": true
                },
                {
                  "name": "authservice-ca",
                  "mountPath": "/etc/qpid-dispatch/authservice-ca",
                  "readOnly": true
                },
                {
                  "name": "router-internal-cert",
                  "mountPath": "/etc/enmasse-certs",
                  "readOnly": true
                }
              ]
                // router.metrics(metrics_image_repo, "32Mi", "router-internal-cert") ],
            }
          ],
          "volumes": [
            {
              "name": "ssl-certs",
              "secret": {
                "secretName": "router-external-cert"
              }
            },
            {
              "name": "authservice-ca",
              "secret": {
                "secretName": "authervice-ca"
              }
            },
            {
              "name": "router-internal-cert",
              "secret": {
                "secretName": "router-internal-cert"
              }
            }
          ]
        }
      }
    }
  }
}
