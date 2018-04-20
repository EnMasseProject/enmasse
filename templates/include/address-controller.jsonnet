local images = import "images.jsonnet";
{
  route::
  {
    "kind": "Route",
    "apiVersion": "route.openshift.io/v1",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "restapi"
    },
    "spec": {
      "to": {
        "kind": "Service",
        "name": "address-controller"
      },
      "tls": {
        "termination": "passthrough"
      }
    }
  },

  service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "address-controller",
      "labels": {
        "app": "enmasse"
      }
    },
    "spec": {
      "ports": [
        {
          "name": "https",
          "port": 443,
          "protocol": "TCP",
          "targetPort": "https"
        }
      ],
      "selector": {
        "name": "address-controller"
      },
      "type": "ClusterIP"
    }
  },

  external_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "restapi-external",
      "labels": {
        "app": "enmasse"
      }
    },
    "spec": {
      "ports": [
        {
          "name": "https",
          "port": 443,
          "protocol": "TCP",
          "targetPort": "https"
        }
      ],
      "selector": {
        "name": "address-controller"
      },
      "type": "LoadBalancer"
    }
  },
  
  deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "name": "address-controller",
        "app": "enmasse"
      },
      "name": "address-controller"
    },
    "spec": {
      "replicas": 1,
      "strategy": {
        "type": "Recreate"
      },
      "template": {
        "metadata": {
          "labels": {
            "name": "address-controller",
            "app": "enmasse"
          }
        },

        "spec": {
          "serviceAccount": "enmasse-admin",
          "containers": [
            {
              "image": images.address_controller,
              "name": "address-controller",
              "env": [
                {
                  "name": "ENABLE_RBAC",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-controller-config",
                      "key": "enableRbac",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "ENABLE_EVENT_LOGGER",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-controller-config",
                      "key": "enableEventLogger",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "ENVIRONMENT",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-controller-config",
                      "key": "environment",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "IMPERSONATE_USER",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-controller-config",
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
                      "name": "address-controller-config",
                      "key": "wildEndpointCertSecret",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "RESYNC_INTERVAL",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-controller-config",
                      "key": "resyncInterval",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "RECHECK_INTERVAL",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "address-controller-config",
                      "key": "recheckInterval",
                      "optional": true
                    }
                  }
                }
              ],
              "volumeMounts": [
                {
                  "name": "address-controller-cert",
                  "mountPath": "/address-controller-cert",
                  "readOnly": true
                },
                {
                  "name": "address-space-definitions",
                  "mountPath": "/address-space-definitions",
                  "readOnly": true
                }
              ],
              "ports": [
                {
                  "name": "https",
                  "containerPort": 8081
                },
                {
                  "name": "http",
                  "containerPort": 8080
                }
              ],
              "livenessProbe": {
                "httpGet": {
                  "path": "/healthz",
                  "scheme": "HTTPS",
                  "port": "https"
                }
              },
              "readinessProbe": {
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
              "name": "address-controller-cert",
              "secret": {
                "secretName": "address-controller-cert"
              }
            },
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
