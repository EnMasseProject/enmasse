local images = import "images.jsonnet";
{
  service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "api-server",
      "labels": {
        "app": "enmasse",
        "component": "api-server"
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
        "component": "api-server"
      },
      "type": "ClusterIP"
    }
  },

  deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": "api-server",
      "labels": {
        "component": "api-server",
        "app": "enmasse"
      }
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "component": "api-server",
            "app": "enmasse"
          }
        },
        "spec": {
          "serviceAccount": "enmasse-admin",
          "containers": [
            {
              "name": "api-server",
              "image": images.api_server,
              "volumeMounts": [
                {
                  "name": "api-server-cert",
                  "mountPath": "/api-server-cert",
                  "readOnly": true
                }
              ],
              "env": [
                {
                  "name": "CLIENT_CA",
                  "valueFrom": {
                    "secretKeyRef": {
                      "name": "api-server-client-ca",
                      "key": "ca.crt",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "REQUEST_HEADER_CLIENT_CA",
                  "valueFrom": {
                    "secretKeyRef": {
                      "name": "api-server-client-ca",
                      "key": "request-header-ca.crt",
                      "optional": true
                    }
                  }
                },
                {
                  "name": "CERT_DIR",
                  "value": "/api-server-cert"
                },
                {
                  "name": "ENABLE_RBAC",
                  "valueFrom": {
                    "configMapKeyRef": {
                      "name": "api-server-config",
                      "key": "enableRbac",
                      "optional": true
                    }
                  }
                }
              ],
              "ports": [
                {
                  "name": "http",
                  "containerPort": 8080
                },
                {
                  "name": "https",
                  "containerPort": 8443
                }
              ],
              "livenessProbe": {
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
              "name": "api-server-cert",
              "secret": {
                "secretName": "api-server-cert"
              }
            }
          ]
        }
      }
    }
  },

  api_service::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "objects": [
      {
        "apiVersion": "apiregistration.k8s.io/v1beta1",
        "kind": "APIService",
        "metadata": {
          "name": "v1alpha1.enmasse.io",
          "labels": {
            "app": "enmasse"
          }
        },
        "spec": {
          "group": "enmasse.io",
          "version": "v1alpha1",
          "insecureSkipTLSVerify": true,
          "groupPriorityMinimum": 1000,
          "versionPriority": 15,
          "service": {
            "name": "api-server",
            "namespace": "${ENMASSE_NAMESPACE}"
          }
        }
      }
    ],
    "parameters": [
      {
        "name": "ENMASSE_NAMESPACE",
        "description": "Namespace where EnMasse is running",
        "value": "enmasse"
      }
    ]
  }
}
