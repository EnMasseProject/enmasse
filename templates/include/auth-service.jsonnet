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
          "name": "amqp-auth",
          "port": 5672,
          "protocol": "TCP",
          "targetPort": "amqp-auth"
        }
      ],
      "selector": {
        "name": "authservice"
      }
    }
  },


  keycloak_authservice::
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
          "name": "amqp-auth",
          "port": 5672,
          "protocol": "TCP",
          "targetPort": "amqp-keycloak"
        },
        {
          "name": "http-keycloak",
          "port": 8080,
          "protocol": "TCP",
          "targetPort": "http-keycloak"
        }
      ],
      "selector": {
        "name": "authservice"
      }
    }
  },

  deployment(keycloak_image_repo, none_authservice_image_repo, keycloak_credentials_secret)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "authservice"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "authservice",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              {
                "image": none_authservice_image_repo,
                "name": "none-authservice",
                "env": [{
                  "name": "LISTENPORT",
                  "value": "56672"
                }],
                "resources": {
                    "requests": {
                        "memory": "64Mi",
                    },
                    "limits": {
                        "memory": "64Mi",
                    }
                },
                "ports": [ { "name": "amqp-auth", "containerPort": 56672 } ],
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "amqp-auth"
                  }
                }
              },
              {
                "image": keycloak_image_repo,
                "name": "keycloak",
                "ports": [
                  {
                    "name": "amqp-keycloak",
                    "containerPort": 5677
                  },
                  {
                    "name": "http-keycloak",
                    "containerPort": 8080
                  }
                ],
                "volumeMounts": [
                  {
                    "name": "keycloak-secret",
                    "mountPath": "/var/run/keycloak",
                    "readOnly": true
                  },
                  {
                    "name": "keycloak-persistence",
                    "mountPath": "/opt/jboss/keycloak/standalone/data",
                  }
                ],
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "amqp-keycloak"
                  }
                }
              }
            ],
            "volumes": [
              {
                "name": "keycloak-secret",
                "secret": {
                  "secretName": keycloak_credentials_secret
                }
              },
              {
                "name": "keycloak-persistence",
                "emptyDir": {}
              }
            ]
          }
        }
      }
    }
}
