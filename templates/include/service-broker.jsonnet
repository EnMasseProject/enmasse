local images = import "images.jsonnet";
{
  service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "service-broker",
      "labels": {
        "app": "enmasse",
        "component": "service-broker"
      },
      "annotations": {
        "service.alpha.openshift.io/serving-cert-secret-name": "service-broker-cert"
      }
    },
    "spec": {
      "ports": [
        {
          "name": "https",
          "port": 8443,
          "protocol": "TCP",
          "targetPort": "https"
        }
      ],
      "selector": {
        "component": "service-broker"
      },
      "type": "ClusterIP"
    }
  },

  deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": "service-broker",
      "labels": {
        "component": "service-broker",
        "app": "enmasse"
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
            "component": "service-broker",
            "app": "enmasse"
          }
        },
        "spec": {
          "serviceAccount": "enmasse-admin",
          "containers": [
            {
              "name": "service-broker",
              "image": images.service_broker,
              "volumeMounts": [
                {
                  "name": "service-broker-cert",
                  "mountPath": "/service-broker-cert",
                  "readOnly": true
                }
              ],
              "env": [
                {
                  "name": "ENABLE_RBAC",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "service-broker-config",
                      "key": "enableRbac"
                    }
                  }
                },
                {
                  "name": "KEYCLOAK_URL",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "service-broker-config",
                      "key": "keycloakUrl"
                    }
                  }
                },
                {
                  "name": "CONSOLE_PREFIX",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "service-broker-config",
                      "key": "consolePrefix"
                    }
                  }
                },
                {
                  "name": "KEYCLOAK_CA",
                  "valueFrom": {
                    "secretKeyRef": {
                      "name": "service-broker-secret",
                      "key": "keycloakCa.crt"
                    }
                  }
                },
                {
                  "name": "KEYCLOAK_ADMIN_USER",
                  "valueFrom": {
                    "secretKeyRef": {
                      "name": "service-broker-secret",
                      "key": "keycloak.username"
                    }
                  }
                },
                {
                  "name": "KEYCLOAK_ADMIN_PASSWORD",
                  "valueFrom": {
                    "secretKeyRef": {
                      "name": "service-broker-secret",
                      "key": "keycloak.password"
                    }
                  }
                },
                {
                  "name": "CERT_DIR",
                  "value": "/service-broker-cert"
                },
                {
                  "name": "LISTEN_PORT",
                  "value": "8443"
                }
              ],
              "ports": [
                {
                  "name": "https",
                  "containerPort": 8443
                }
              ],
              "livenessProbe": {
                "httpGet": {
                  "path": "/healthz",
                  "scheme": "HTTPS",
                  "port": "https"
                }
              }
            }
          ],
          "volumes": [
            {
              "name": "service-broker-cert",
              "secret": {
                "secretName": "service-broker-cert"
              }
            }
          ]
        }
      }
    }
  }
}
