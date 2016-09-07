{
  "vanilla": {
    local multicast = std.extVar("multicast"),
    "shared_cluster": true,
    "template": {
      "apiVersion": "v1",
      "kind": "Template",
      "metadata": {
        "name": "vanilla"
      },
      "objects": {
        "apiVersion": "v1",
        "kind": "ReplicationController",
        "metadata": {
          "labels": {
            "role": "controller-",
            "address": "${ADDRESS}"
          }
        },
        "spec": {
          "replicas": 1,
          "selector": {
            "address": "${ADDRESS}"
          },
          "template": {
            "metadata": {
              "labels": {
                "role": "broker",
                "capability": "router",
                "address": "${ADDRESS}"
              }
            },
            "spec": {
              "volumes": [
                {
                  "name": "vol-vanilla",
                  "emptyDir": {}
                }
              ],
              local containers_common = [
                {
                  "name": "broker",
                  "image": "enmasseproject/artemis:latest",
                  "ports": [
                    {
                      "name": "amqp",
                      "containerPort": 5673
                    },
                    {
                      "name": "core",
                      "containerPort": 61616
                    }
                  ],
                  "volumeMounts": [
                    {
                      "name": "vol-vanilla",
                      "mountPath": "/var/run/artemis"
                    }
                  ],
                  [if multicast != "true" then "lifecycle"]: {
                    "preStop": {
                      "exec": {
                        "command": [
                          "/artemis-shutdown-hook/bin/artemis-shutdown-hook"
                        ]
                      }
                    }
                  }
                },
                {
                  "name": "router",
                  "image": "gordons/qdrouterd:latest",
                  "ports": [
                    {
                      "name": "amqp",
                      "containerPort": 5672
                    }
                  ]
                }
              ],
              "containers": (if multicast == "true"
                then containers_common + [{ "name": "forwarder", "image": "enmasseproject/topic-forwarder:latest" }]
                else containers_common)
            }
          }
        }
      }
    }
  }
}
