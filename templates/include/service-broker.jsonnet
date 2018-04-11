local images = import "images.jsonnet";
{
  "apiVersion": "v1",
  "kind": "Template",
  "metadata": {
    "name": "enmasse-service-broker"
  },
  "objects": [
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
            "port": "${{LISTEN_PORT}}",
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
            "serviceAccount": "${SERVICE_BROKER_SA}",
            "containers": [
              {
                "name": "service-broker",
                "image": "${SERVICE_BROKER_IMAGE}",
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
                    "value": "${ENABLE_RBAC}"
                  },
                  {
                    "name": "IMPERSONATE_USER",
                    "value": "${IMPERSONATE_USER}"
                  },
                  {
                    "name": "KEYCLOAK_CA",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "${KEYCLOAK_CA_SECRET}",
                        "key": "tls.crt"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_ADMIN_USER",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "${KEYCLOAK_CREDENTIALS_SECRET}",
                        "key": "admin.username"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_ADMIN_PASSWORD",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "${KEYCLOAK_CREDENTIALS_SECRET}",
                        "key": "admin.password"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_URL",
                    "valueFrom": {
                      "configMapKeyRef": {
                        "name": "${KEYCLOAK_CONFIG}",
                        "key": "httpUrl"
                      }
                    }
                  },
                  {
                    "name": "CERT_DIR",
                    "value": "/service-broker-cert"
                  },
                  {
                    "name": "LISTEN_PORT",
                    "value": "${LISTEN_PORT}"
                  }
                ],
                "ports": [
                  {
                    "name": "https",
                    "containerPort": "${{LISTEN_PORT}}"
                  }
                ],
                "livenessProbe": {
                  "httpGet": {
                    "path": "/healthz",
                    "scheme": "HTTPS",
                    "ports": "https"
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
  ],
  "parameters": [
    {
      "name": "SERVICE_BROKER_SA",
      "description": "Service broker service account",
      "value": "enmasse-admin",
    },
    {
      "name": "SERVICE_BROKER_IMAGE",
      "description": "Container image to use for service broker",
      "value": images.service_broker
    },
    {
      "name": "IMPERSONATE_USER",
      "description": "User to impersonate when creating resources",
    },
    {
      "name": "KEYCLOAK_CREDENTIALS_SECRET",
      "description": "Secret containing credentials for keycloak",
      "value": "keycloak-credentials",
    },
    {
      "name": "KEYCLOAK_CA_SECRET",
      "description": "Secret containing keycloak certificate",
      "value": "standard-authservice-cert",
    },
    {
      "name": "ENABLE_RBAC",
      "description": "Enable RBAC authentication and authorization of API",
      "value": "false",
    },
    {
      "name": "LISTEN_PORT",
      "description": "HTTP port to listen to",
      "value": "8443",
    }
    {
      "name": "KEYCLOAK_CONFIG",
      "description": "Name of configmap for keycloak configuration",
      "value": "keycloak-config"
    }
  ]
}
