local router = import "router.jsonnet";
local console = import "console.jsonnet";
local common = import "common.jsonnet";
{
  service(name, addressSpace, ports)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": name,
        "labels": {
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace
        }
      },
      "spec": {
        "ports": ports,
        "selector": {
          "name": "admin"
        }
      }
    },

  services(addressSpace)::
  [
    self.service("ragent", addressSpace, [{"name": "amqp", "port": 5671, "targetPort": 55671}]),
    self.service("configuration", addressSpace, [{"name": "amqp", "port": 5672}]),
    self.service("queue-scheduler", addressSpace, [{"name": "amqp", "port": 5672, "targetPort": 55667}]),
    self.service("console", addressSpace, [{"name": "amqp-ws", "port": 5672, "targetPort": 56720}, {"name": "http", "port": 8080}])
  ],

  deployment(addressSpace, configserv_image, ragent_image, scheduler_image, console_image, auth_service_ca_secret)::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "labels": {
        "app": "enmasse",
        "name": "admin",
      },
      "annotations": {
        "addressSpace": addressSpace,
        "io.enmasse.certSecretName": "admin-internal-cert"
      },
      "name": "admin"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "admin",
            "app": "enmasse",
          },
          "annotations": {
            "addressSpace": addressSpace
          }
        },
        "spec": {
          "containers": [
            {
              "image": ragent_image,
              "name": "ragent",
              "env": [
                 {
                   "name": "CONFIGURATION_SERVICE_HOST",
                   "value": "localhost"
                 },
                 {
                   "name": "CONFIGURATION_SERVICE_PORT",
                   "value": "5672"
                 },
                 {
                   "name": "PROBE_PORT",
                   "value": "8888"
                 }
              ],
              "resources": {
                "requests": {
                  "memory": "64Mi",
                },
                "limits": {
                  "memory": "64Mi",
                }
              },
              "ports": [
                {
                  "name": "amqp",
                  "containerPort": 55671,
                  "protocol": "TCP"
                },
                {
                  "name": "http",
                  "containerPort": 8888,
                  "protocol": "TCP"
                }
              ],
              "livenessProbe": {
                "httpGet": {
                  "port": "http"
                }
              },
              "volumeMounts": [
                               {
                                 "name": "admin-internal-cert",
                                 "mountPath": "/etc/enmasse-certs",
                                 "readOnly": true
                               }
                             ]
            },
            common.container("queue-scheduler", scheduler_image, "amqp", 55667, "128Mi", [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      }]),
            console.container(console_image, [
                      {
                        "name": "CONFIGURATION_SERVICE_HOST",
                        "value": "localhost"
                      },
                      {
                        "name": "CONFIGURATION_SERVICE_PORT",
                        "value": "5672"
                      },
                      {
                        "name": "ADDRESS_SPACE_SERVICE_HOST",
                        "value": "${ADDRESS_SPACE_SERVICE_HOST}"
                      },
                      {
                        "name": "ADDRESS_SPACE",
                        "value": addressSpace
                      }
                      ]) + {
                        "volumeMounts": [
                          {
                            "name": "authservice-ca",
                            "mountPath": "/opt/console/authservice-ca",
                            "readOnly": true
                          },
                          {
                            "name": "admin-internal-cert",
                            "mountPath": "/etc/enmasse-certs",
                            "readOnly": true
                          }
                        ]
                      },
            common.container("configserv", configserv_image, "amqp", 5672, "256Mi", []),
          ],
          "volumes": [
            {
              "name": "authservice-ca",
              "secret": {
                "secretName": auth_service_ca_secret
              }
            },
            {
                "name": "admin-internal-cert",
                "secret": {
                    "secretName": "admin-internal-cert"
                }
            }
          ]
        }
      }
    }
  }
}
