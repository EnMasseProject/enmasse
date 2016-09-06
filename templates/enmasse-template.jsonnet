local secure = std.extVar("secure");
local templateName = (if secure == "true" then "enmasse-secure" else "enmasse");
local gen = import "broker-common.jsonnet";
{
  "apiVersion": "v1",
  "kind": "Template",
  "metadata": {
    "name": templateName
  },
  "objects": [ gen.generate_template("false", "false"),
               gen.generate_template("false", "true"),
               gen.generate_template("true", "false"),
               gen.generate_template("true", "true") ] + [
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "configuration"
      },
      "spec": {
        "ports": [
          {
            "port": 5672,
            "protocol": "TCP",
            "targetPort": 5672
          }
        ],
        "selector": {
          "name": "configmap-bridge"
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "messaging"
      },
      "spec": {
        "ports": [
          {
            "port": 5672,
            "protocol": "TCP",
            "targetPort": 5672
          }
        ],
        "selector": {
          "capability": "router"
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "ragent"
      },
      "spec": {
        "ports": [
          {
            "port": 55672,
            "protocol": "TCP",
            "targetPort": 55672
          }
        ],
        "selector": {
          "name": "ragent"
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "ReplicationController",
      "metadata": {
        "labels": {
          "name": "configmap-bridge"
        },
        "name": "configmap-bridge"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "configmap-bridge"
        },
        "template": {
          "metadata": {
            "labels": {
              "name": "configmap-bridge"
            }
          },
          "spec": {
            "containers": [
              {
                "image": "${CONFIGMAP_BRIDGE_IMAGE}",
                "name": "bridge",
                "ports": [
                  {
                    "containerPort": 5672,
                    "protocol": "TCP"
                  }
                ]
              }
            ]
          }
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "ReplicationController",
      "metadata": {
        "labels": {
          "name": "qdrouterd"
        },
        "name": "qdrouterd"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "qdrouterd"
        },
        "template": {
          "metadata": {
            "labels": {
              "capability": "router",
              "name": "qdrouterd"
            }
          },
          "spec": {
            "containers": [
              {
                "image": "${ROUTER_IMAGE}",
                "name": "master",
                "ports": [
                  {
                    "containerPort": 5672,
                    "protocol": "TCP"
                  }
                ]
              }
            ]
          }
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "ReplicationController",
      "metadata": {
        "labels": {
          "name": "ragent"
        },
        "name": "ragent"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "ragent"
        },
        "template": {
          "metadata": {
            "labels": {
              "name": "ragent"
            }
          },
          "spec": {
            "containers": [
              {
                "image": "${RAGENT_IMAGE}",
                "name": "master",
                "ports": [
                  {
                    "containerPort": 55672,
                    "protocol": "TCP"
                  }
                ]
              }
            ]
          }
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "ReplicationController",
      "metadata": {
        "labels": {
          "name": "storage-controller"
        },
        "name": "storage-controller"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "storage-controller"
        },
        "template": {
          "metadata": {
            "labels": {
              "name": "storage-controller"
            }
          },
          "spec": {
            "serviceAccount": "deployer",
            "containers": [
              {
                "image": "${STORAGE_CONTROLLER_IMAGE}",
                "name": "master"
              }
            ]
          }
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "ConfigMap",
      "metadata": {
        "name": "maas"
      },
      "data": {
        "json": "{\n    \"myqueue\": { \"store_and_forward\":true, \"multicast\":false, \"flavor\": \"queue-inmemory\" },\n    \"mytopic\": { \"store_and_forward\":true, \"multicast\":true, \"flavor\": \"topic-inmemory\" },\n    \"anycast\": { \"store_and_forward\":false, \"multicast\":false },\n    \"broadcast\": { \"store_and_forward\":false, \"multicast\":true }\n}\n"
      }
    }
  ],
  "parameters": [
    {
      "name": "BROKER_IMAGE",
      "value": "enmasseproject/artemis:latest",
      "description": "The image to use for the broker"
    },
    {
      "name": "ROUTER_IMAGE",
      "description": "The image to use for the router",
      "value": "gordons/qdrouterd:latest"
    },
    {
      "name": "CONFIGMAP_BRIDGE_IMAGE",
      "description": "The image to use for the configmap notification bridge",
      "value": "enmasseproject/configmap-bridge:latest"
    },
    {
      "name": "STORAGE_CONTROLLER_IMAGE",
      "description": "The docker image to use for the storage controller",
      "value": "enmasseproject/storage-controller:latest"
    },
    {
      "name": "RAGENT_IMAGE",
      "description": "The image to use for the router agent",
      "value": "enmasseproject/ragent:latest"
    }
  ]
}
