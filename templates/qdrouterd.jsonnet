{ 
  local port = {
    "name": "amqp",
    "port": 5672,
    "protocol": "TCP",
    "targetPort": 5672
  },
  local securePort = {
    "name": "amqps",
    "port": 5671,
    "protocol": "TCP",
    "targetPort": 5671
  },
  generate(secure)::
  [
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "messaging"
      },
      "spec": {
        "ports": if secure == "true" then [port, securePort] else [port],
        "selector": {
          "capability": "router"
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
            local routerPort = {
                "name": "amqp",
                "containerPort": 5672,
                "protocol": "TCP"
            },
            local secureRouterPort = {
                "name": "amqps",
                "containerPort": 5671,
                "protocol": "TCP"
            },
            "containers": [
              {
                "image": "${QDROUTER_IMAGE}",
                "name": "router",
                "ports": if secure == "true"
                  then [routerPort, secureRouterPort] 
                  else [routerPort],
                [if secure == "true" then "volumeMounts"]: [
                  {
                    "name": "ssl-certs",
                    "mountPath": "/etc/qpid-dispatch/ssl",
                    "readOnly": true
                  }
                ]
              }
            ],
            [if secure == "true" then "volumes"]: [
              {
                "name": "ssl-certs",
                "secret": {
                  "secretName": "qdrouterd-certs"
                }
              }
            ]
          }
        }
      }
    }
  ]
}
