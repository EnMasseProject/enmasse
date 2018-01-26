local common = import "common.jsonnet";
local images = import "images.jsonnet";
{
  envVars::
    [
      common.env("AUTHENTICATION_SERVICE_HOST", "${AUTHENTICATION_SERVICE_HOST}"),
      common.env("AUTHENTICATION_SERVICE_PORT", "${AUTHENTICATION_SERVICE_PORT}"),
      common.env("AUTHENTICATION_SERVICE_CLIENT_SECRET", "${AUTHENTICATION_SERVICE_CLIENT_SECRET}"),
      common.env("AUTHENTICATION_SERVICE_SASL_INIT_HOST", "${AUTHENTICATION_SERVICE_SASL_INIT_HOST}")
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

  keycloak_controller_deployment(keycloak_controller_image, keycloak_credentials_secret, keycloak_controller_configmap, cert_secret)::
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
                        "memory": "256Mi",
                    },
                    "limits": {
                        "memory": "256Mi",
                    }
                },
                "env": [
                  {
                    "name": "KEYCLOAK_HOSTNAME",
                    "valueFrom": {
                      "configMapKeyRef": {
                        "name": keycloak_controller_configmap,
                        "key": "hostname"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_PORT",
                    "valueFrom": {
                      "configMapKeyRef": {
                        "name": keycloak_controller_configmap,
                        "key": "port"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_ADMIN_USER",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": keycloak_credentials_secret,
                        "key": "admin.username"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_ADMIN_PASSWORD",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": keycloak_credentials_secret,
                        "key": "admin.password"
                      }
                    }
                  },
                  {
                    "name": "KEYCLOAK_CERT",
                    "valueFrom": {
                      "secretKeyRef": {
                        "name": cert_secret,
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



  keycloak_deployment(keycloak_plugin_image, keycloak_image, keycloak_credentials_secret, cert_secret_name, pvc_claim_name, podspec_extra={})::
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
                "image": keycloak_plugin_image,
                "name": "keycloak-plugin",
                "env": [
                  common.env("KEYCLOAK_DIR", "/opt/jboss/keycloak")
                ],
                "volumeMounts": [
                  common.volume_mount("keycloak-providers", "/opt/jboss/keycloak/providers"),
                  common.volume_mount("keycloak-configuration", "/opt/jboss/keycloak/standalone/configuration"),
                  common.volume_mount(cert_secret_name, "/opt/enmasse/cert")
                ],
              }
            ],
            "containers": [
              {
                "image": keycloak_image,
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
                  common.volume_mount("keycloak-providers", "/opt/jboss/keycloak/providers"),
                  common.volume_mount("keycloak-configuration", "/opt/jboss/keycloak/standalone/configuration"),
                  common.volume_mount(cert_secret_name, "/opt/enmasse/cert"),
                  common.volume_mount("keycloak-persistence", "/opt/jboss/keycloak/standalone/data")
                ],
                "livenessProbe": common.http_probe("https", "/auth", "HTTPS", 120),
                "readinessProbe": common.http_probe("https", "/auth", "HTTPS", 120)
              }
            ],
            "volumes": [
              common.secret_volume(cert_secret_name, cert_secret_name),
              common.persistent_volume("keycloak-persistence", pvc_claim_name),
              common.empty_volume("keycloak-configuration"),
              common.empty_volume("keycloak-providers")
            ]
          } + podspec_extra
        }
      }
    },

  keycloak_pvc(name, capacity)::
  {
    "apiVersion": "v1",
    "kind": "PersistentVolumeClaim",
    "metadata": {
      "name": name,
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
          "storage": capacity
        }
      }
    }
  },

  keycloak_route(hostname)::
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
        "host": hostname,
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
                  common.volume_mount(cert_secret_name, "/opt/none-authservice/cert")
                ]
              },
            ],
            "volumes": [
              common.secret_volume(cert_secret_name, cert_secret_name)
            ]
          }
        }
      }
    },

  keycloak_controller_config::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": "keycloak-controller-config",
      "labels": {
        "app": "enmasse"
      }
    },
    "data": {
      "hostname": "standard-authservice",
      "port": "8443"
    }
  },

  local me = self,
  keycloak_controller_kubernetes::
    me.keycloak_controller_deployment(images.keycloak_controller, "keycloak-credentials", "keycloak-controller-config", "keycloak-cert"),

  keycloak_kubernetes::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      me.keycloak_pvc("keycloak-pvc", "2Gi"),
      me.keycloak_deployment(images.keycloak_plugin, images.keycloak, "keycloak-credentials", "standard-authservice-cert",
        "keycloak-pvc", {
              "securityContext": {
                "runAsUser": 0,
                "fsGroup": 0
              }
        }),
      me.keycloak_controller_deployment(images.keycloak_controller, "keycloak-credentials", "keycloak-controller-config", "standard-authservice-cert"),
      me.keycloak_controller_config,
      me.standard_authservice,
      me.standard_authservice_external
    ],
  },

  keycloak_openshift::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "objects": [
      me.keycloak_pvc("keycloak-pvc", "${KEYCLOAK_STORAGE_CAPACITY}"),
      me.keycloak_deployment("${KEYCLOAK_PLUGIN_IMAGE}", "${STANDARD_AUTHSERVICE_IMAGE}", "${KEYCLOAK_SECRET_NAME}", "${KEYCLOAK_CERT_SECRET_NAME}", "keycloak-pvc"),
      me.keycloak_controller_deployment("${KEYCLOAK_CONTROLLER_IMAGE}", "${KEYCLOAK_SECRET_NAME}", "${KEYCLOAK_CONTROLLER_CONFIGMAP_NAME}", "${KEYCLOAK_CERT_SECRET_NAME}"),
      me.keycloak_controller_config,
      me.standard_authservice,
      me.keycloak_route("${KEYCLOAK_ROUTE_HOSTNAME}")
    ],
    "parameters": [
      {
        "name": "KEYCLOAK_PLUGIN_IMAGE",
        "description": "The docker image to use for the keycloak plugin image",
        "value": images.keycloak_plugin
      },
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
        "name": "KEYCLOAK_CONTROLLER_CONFIGMAP_NAME",
        "description": "The configmap used to configure the keycloak controller",
        "value": "keycloak-controller-config"
      },
      {
        "name": "KEYCLOAK_CERT_SECRET_NAME",
        "description": "The secret containing the tls certificate and key",
        "value": "standard-authservice-cert"
      },
      {
        "name": "KEYCLOAK_ROUTE_HOSTNAME",
        "description": "The hostname to use for the public keycloak route",
        "value": ""
      },
      {
        "name": "KEYCLOAK_STORAGE_CAPACITY",
        "description": "The amount of storage to request for Keycloak data",
        "value": "2Gi"
      }
    ]
  },

  none_kubernetes::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      me.none_deployment(images.none_authservice, "none-authservice-cert"),
      me.none_authservice,
    ],
  },

  none_openshift::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "objects": [
      me.none_deployment("${NONE_AUTHSERVICE_IMAGE}", "${NONE_AUTHSERVICE_CERT_SECRET_NAME}"),
      me.none_authservice,
    ],
    "parameters": [
      {
        "name": "NONE_AUTHSERVICE_IMAGE",
        "description": "The docker image to use for the 'none' auth service",
        "value": images.none_authservice
      },
      {
        "name": "NONE_AUTHSERVICE_CERT_SECRET_NAME",
        "description": "The secret to use for the none-authservice certificate",
        "value": "none-authservice-cert"
      },
    ]
  },
}
