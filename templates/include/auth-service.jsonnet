local images = import "images.jsonnet";
{
  envVars::
    [
      {
        "name": "AUTHENTICATION_SERVICE_HOST",
        "value": "${AUTHENTICATION_SERVICE_HOST}"
      },
      {
        "name": "AUTHENTICATION_SERVICE_PORT",
        "value": "${AUTHENTICATION_SERVICE_PORT}"
      },
      {
        "name": "AUTHENTICATION_SERVICE_CA_SECRET",
        "value": "${AUTHENTICATION_SERVICE_CA_SECRET}"
      },
      {
        "name": "AUTHENTICATION_SERVICE_CLIENT_SECRET",
        "value": "${AUTHENTICATION_SERVICE_CLIENT_SECRET}"
      },
      {
        "name": "AUTHENTICATION_SERVICE_SASL_INIT_HOST",
        "value": "${AUTHENTICATION_SERVICE_SASL_INIT_HOST}"
      }
    ],

  none_authservice::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "none-authservice",
      "labels": {
        "app": "enmasse"
      }
    },
    "spec": {
      "ports": [
        {
          "name": "amqps",
          "port": 5671,
          "protocol": "TCP",
          "targetPort": "amqps"
        }
      ],
      "selector": {
        "name": "none-authservice"
      }
    }
  },


  standard_authservice::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "standard-authservice",
      "labels": {
        "app": "enmasse"
      }
    },
    "spec": {
      "ports": [
        {
          "name": "amqp",
          "port": 5672,
          "protocol": "TCP",
          "targetPort": "amqp"
        },
        {
          "name": "http",
          "port": 8080,
          "protocol": "TCP",
          "targetPort": "http"
        }
      ],
      "selector": {
        "name": "keycloak"
      }
    }
  },

  keycloak_controller_deployment(keycloak_controller_image, keycloak_credentials_secret)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "keycloak-controller"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "keycloak-controller",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              {
                "image": keycloak_controller_image,
                "name": "keycloak-controller",
                "resources": {
                    "requests": {
                        "memory": "128Mi",
                    },
                    "limits": {
                        "memory": "128Mi",
                    }
                },
                "env": [
                  {
                    "name": "STANDARD_AUTHSERVICE_ADMIN_USER",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": keycloak_credentials_secret,
                        "key": "admin.username"
                      }
                    }
                  },
                  {
                    "name": "STANDARD_AUTHSERVICE_ADMIN_PASSWORD",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": keycloak_credentials_secret,
                        "key": "admin.password"
                      }
                    }
                  }
                ]
              }
            ],
          }
        }
      }
    },


  keycloak_deployment(keycloak_image, keycloak_credentials_secret, cert_secret_name)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "keycloak"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "keycloak",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              {
                "image": keycloak_image,
                "name": "keycloak",
                "ports": [
                  {
                    "name": "amqp",
                    "containerPort": 5672
                  },
                  {
                    "name": "http",
                    "containerPort": 8080
                  }
                ],
                "env": [
                  {
                    "name": "KEYCLOAK_USER",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": keycloak_credentials_secret,
                        "key": "admin.username"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_PASSWORD",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": keycloak_credentials_secret,
                        "key": "admin.password"
                      }
                    }
                  }
                ],
                "volumeMounts": [
                  {
                    "name": "keycloak-persistence",
                    "mountPath": "/opt/jboss/keycloak/standalone/data",
                  },
                  {
                    "name": cert_secret_name,
                    "mountPath": "/opt/jboss/keycloak/standalone/cert",
                  }
                ],
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "amqp"
                  },
                  "initialDelaySeconds": 120
                }
              }
            ],
            "volumes": [
              {
                "name": "keycloak-persistence",
                "emptyDir": {}
              },
              { "name": cert_secret_name,
                "secret": {
                  "secretName": cert_secret_name
                          }
              }
            ]
          }
        }
      }
    },

  none_deployment(none_authservice_image, cert_secret_name)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "none-authservice"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "none-authservice",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              {
                "image": none_authservice_image,
                "name": "none-authservice",
                "env": [{
                  "name": "LISTENPORT",
                  "value": "5671"
                }],
                "resources": {
                    "requests": {
                        "memory": "48Mi",
                    },
                    "limits": {
                        "memory": "48Mi",
                    }
                },
                "ports": [ { "name": "amqps", "containerPort": 5671 } ],
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "amqps"
                  }
                },
                "volumeMounts": [
                  {
                    "name": cert_secret_name,
                    "mountPath": "/opt/none-authservice/cert",
                  }
                ]
              },
            ],
            "volumes": [ { "name": cert_secret_name,
                           "secret": {
                             "secretName": cert_secret_name
                                     }
                         } ]
          }
        }
      }
    },

  local me = self,
  kubernetes::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      me.keycloak_deployment(images.keycloak, "keycloak-credentials", "standard-authservice-cert"),
      me.keycloak_controller_deployment(images.keycloak_controller, "keycloak-credentials"),
      me.standard_authservice
    ],
  },

  openshift::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "objects": [
      me.keycloak_deployment("${STANDARD_AUTHSERVICE_IMAGE}", "${KEYCLOAK_SECRET_NAME}", "${STANDARD_AUTHSERVICE_SECRET_NAME}"),
      me.keycloak_controller_deployment("${KEYCLOAK_CONTROLLER_IMAGE}", "${KEYCLOAK_SECRET_NAME}"),
      me.standard_authservice
    ],
    "parameters": [
      {
        "name": "STANDARD_AUTHSERVICE_IMAGE",
        "description": "The docker image to use for the 'standard' auth service",
        "value": images.keycloak
      },
      {
        "name": "KEYCLOAK_SECRET_NAME",
        "description": "The secret where keycloak credentials are stored",
        "value": "keycloak-credentials"
      },
      {
        "name": "KEYCLOAK_CONTROLLER_IMAGE",
        "description": "The docker image to use for the keycloak controller",
        "value": images.keycloak_controller
      },
      {
        "name": "STANDARD_AUTHSERVICE_SECRET_NAME",
        "description": "The secret containing the tls certificate and key",
        "value": "standard-authservice-cert"
      },
    ]
  },
}
