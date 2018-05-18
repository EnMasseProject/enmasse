local images = import "images.jsonnet";
{
  deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "name": "address-space-controller",
        "app": "enmasse"
      },
      "name": "address-space-controller"
    },
    "spec": {
      "replicas": 1,
      "strategy": {
        "type": "Recreate"
      },
      "template": {
        "metadata": {
          "labels": {
            "name": "address-space-controller",
            "app": "enmasse"
          }
        },

        "spec": {
          "serviceAccount": "enmasse-admin",
          "containers": [
            {
              "image": images.address_space_controller,
              "name": "address-space-controller",
              "env": [
                {
                  "name": "ENABLE_RBAC",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "enableRbac",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "ENABLE_EVENT_LOGGER",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "enableEventLogger",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "ENVIRONMENT",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "environment",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "IMPERSONATE_USER",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "impersonateUser",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "ADDRESS_CONTROLLER_SA",
                  "value": "enmasse-admin"
                },
                {
                  "name": "ADDRESS_SPACE_ADMIN_SA",
                  "value": "address-space-admin"
                },
                {
                  "name": "TEMPLATE_DIR",
                  "value": "/address-space-definitions"
                },
                {
                  "name": "STANDARD_AUTHSERVICE_CONFIG",
                  "value": "keycloak-config"
                },
                {
                  "name": "WILDCARD_ENDPOINT_CERT_SECRET",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "wildcardEndpointCertSecret",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "RESYNC_INTERVAL",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "resyncInterval",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "RECHECK_INTERVAL",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-space-controller-config",
                      "key": "recheckInterval",
                      "optional": true
                    }
                  }
                }
              ],
              "volumeMounts": [
                {
                  "name": "address-space-definitions",
                  "mountPath": "/address-space-definitions",
                  "readOnly": true
                }
              ],
              "ports": [
                {
                  "name": "http",
                  "containerPort": 8080
                }
              ],
              "livenessProbe": {
                "httpGet": {
                  "path": "/healthz",
                  "scheme": "HTTP",
                  "port": "http"
                }
              },
              "readinessProbe": {
                "httpGet": {
                  "path": "/healthz",
                  "scheme": "HTTP",
                  "port": "http"
                }
              },
              "resources": {
                "requests": {
                  "memory": "128Mi"
                },
                "limits": {
                  "memory": "256Mi"
                }
              }
            }
          ],
          "volumes": [
            {
              "name": "address-space-definitions",
              "configMap": {
                "name": "address-space-definitions"
              }
            }
          ]
        }
      }
    }
  }
}
