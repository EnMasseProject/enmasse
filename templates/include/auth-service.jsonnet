local common = import "common.jsonnet";
local images = import "images.jsonnet";
{
  envVars::
    [
      common.env("AUTHENTICATION_SERVICE_HOST", "${AUTHENTICATION_SERVICE_HOST}"),
      common.env("AUTHENTICATION_SERVICE_PORT", "${AUTHENTICATION_SERVICE_PORT}"),
      common.env("AUTHENTICATION_SERVICE_CLIENT_SECRET", "${AUTHENTICATION_SERVICE_CLIENT_SECRET}"),
      common.env("AUTHENTICATION_SERVICE_SASL_INIT_HOST", "${AUTHENTICATION_SERVICE_SASL_INIT_HOST}"),
      common.env("AUTHENTICATION_SERVICE_OAUTH_URL", "${AUTHENTICATION_SERVICE_OAUTH_URL}")
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
          "name": "amqps",
          "port": 5671,
          "protocol": "TCP",
          "targetPort": "amqps"
        },
        {
          "name": "https",
          "port": 8443,
          "protocol": "TCP",
          "targetPort": "https"
        }
      ],
      "selector": {
        "name": "keycloak"
      }
    }
  },

  standard_authservice_external::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "standard-authservice-external",
      "labels": {
        "app": "enmasse"
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
        "name": "keycloak"
      },
      "type": "LoadBalancer"
    }
  },

  keycloak_controller_deployment::
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
                "image": images.keycloak_controller,
                "name": "keycloak-controller",
                "resources": {
                    "requests": {
                        "memory": "256Mi",
                    },
                    "limits": {
                        "memory": "256Mi",
                    }
                },
                "env": [
                  {
                    "name": "KEYCLOAK_URI",
                    "valueFrom": {
                      "configMapKeyRef": {
                        "name": "keycloak-config",
                        "key": "httpUrl"
                      }
                    }
                  },
                  {
                    "name": "OAUTH_IDENTITY_PROVIDER_CLIENT_ID",
                    "valueFrom": {
                      "configMapKeyRef": {
                        "optional": true,
                        "name": "keycloak-config",
                        "key": "identityProviderClientId"
                      }
                    }
                  },
                  {
                    "name": "OAUTH_IDENTITY_PROVIDER_CLIENT_SECRET",
                    "valueFrom": {
                      "configMapKeyRef": {
                        "optional": true,
                        "name": "keycloak-config",
                        "key": "identityProviderClientSecret"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_ADMIN_USER",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "keycloak-credentials",
                        "key": "admin.username"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_ADMIN_PASSWORD",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "keycloak-credentials",
                        "key": "admin.password"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_CERT",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "standard-authservice-cert",
                        "key": "tls.crt"
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

  keycloak_deployment::
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
            "initContainers": [
              {
                "image": images.keycloak_plugin,
                "name": "keycloak-plugin",
                "env": [
                  common.env("KEYCLOAK_DIR", "/opt/jboss/keycloak")
                ],
                "volumeMounts": [
                  common.volume_mount("keycloak-providers", "/opt/jboss/keycloak/providers"),
                  common.volume_mount("keycloak-configuration", "/opt/jboss/keycloak/standalone/configuration"),
                  common.volume_mount("standard-authservice-cert", "/opt/enmasse/cert")
                ],
              }
            ],
            "containers": [
              {
                "image": images.keycloak,
                "name": "keycloak",
                "ports": [
                  common.container_port("amqps", 5671),
                  common.container_port("https", 8443)
                ],
                "resources": {
                    "requests": {
                        "memory": "2Gi",
                    },
                    "limits": {
                        "memory": "2Gi",
                    }
                },
                "env": [
                  common.env("JAVA_OPTS", "-Dvertx.cacheDirBase=/tmp -Djboss.bind.address=0.0.0.0 -Djava.net.preferIPv4Stack=true -Xms512m -Xmx1024m"),
                  {
                    "name": "KEYCLOAK_USER",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "keycloak-credentials",
                        "key": "admin.username"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_PASSWORD",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": "keycloak-credentials",
                        "key": "admin.password"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_SASL_XOAUTH_BASE_URI", 
                    "valueFrom": {
                      "configMapKeyRef": {
                        "name": "keycloak-config",
                        "key": "httpUrl"
                      }
                    }
                  },
                ],
                "volumeMounts": [
                  common.volume_mount("keycloak-providers", "/opt/jboss/keycloak/providers"),
                  common.volume_mount("keycloak-configuration", "/opt/jboss/keycloak/standalone/configuration"),
                  common.volume_mount("standard-authservice-cert", "/opt/enmasse/cert"),
                  common.volume_mount("keycloak-persistence", "/opt/jboss/keycloak/standalone/data")
                ],
                "livenessProbe": common.http_probe("https", "/auth", "HTTPS", 120),
                "readinessProbe": common.http_probe("https", "/auth", "HTTPS", 60)
              }
            ],
            "volumes": [
              common.secret_volume("standard-authservice-cert", "standard-authservice-cert"),
              common.persistent_volume("keycloak-persistence", "keycloak-pvc"),
              common.empty_volume("keycloak-configuration"),
              common.empty_volume("keycloak-providers")
            ]
          }
        }
      }
    },

  keycloak_pvc::
  {
    "apiVersion": "v1",
    "kind": "PersistentVolumeClaim",
    "metadata": {
      "name": "keycloak-pvc",
      "labels": {
        "app": "enmasse"
      }
    },
    "spec": {
      "accessModes": [
        "ReadWriteOnce"
      ],
      "resources": {
        "requests": {
          "storage": "5Gi"
        }
      }
    }
  },

  keycloak_route::
    {
      "kind": "Route",
      "apiVersion": "v1",
      "metadata": {
          "labels": {
            "app": "enmasse"
          },
          "name": "keycloak"
      },
      "spec": {
        "to": {
            "kind": "Service",
            "name": "standard-authservice"
        },
        "port": {
            "targetPort": "https"
        },
        "tls": {
          "termination": "passthrough"
        }
      }
    },


  none_deployment::
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
                "image": images.none_authservice,
                "name": "none-authservice",
                "env": [
                  common.env("LISTENPORT", "5671")
                ],
                "resources": {
                    "requests": {
                        "memory": "48Mi",
                    },
                    "limits": {
                        "memory": "48Mi",
                    }
                },
                "ports": [
                  common.container_port("amqps", 5671)
                ],
                "livenessProbe": common.tcp_probe("amqps", 60),
                "volumeMounts": [
                  common.volume_mount("none-authservice-cert", "/opt/none-authservice/cert")
                ]
              },
            ],
            "volumes": [
              common.secret_volume("none-authservice-cert", "none-authservice-cert")
            ]
          }
        }
      }
    },
}
