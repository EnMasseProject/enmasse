local multicast = std.extVar("multicast");
local persistence = std.extVar("persistence");
local addrtype = (if multicast == "true" then "topic" else "queue");
local templateName = "%s-%s" % [addrtype, (if persistence == "true" then "persisted" else "inmemory")];
local containerEnv = (if multicast == "true" then [ { name: "TOPIC_NAME", value: "${ADDRESS}" } ] else [ { name: "QUEUE_NAME", value: "${ADDRESS}" }]);
{ 
  "apiVersion": "v1",
  "kind": "Template",
  "metadata": {
    "name": templateName
  },
  "objects": [
    {
      "apiVersion": "v1",
      "kind": "ReplicationController",
      "metadata": {
        "name": "controller-${ADDRESS}"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "address": "${ADDRESS}",
          "role": "broker"
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
                "name": "vol-%s" % [templateName],
                "emptyDir": {}
              }
            ],
            local containers_common = [
              {
                "name": "broker",
                "image": "${BROKER_IMAGE}",
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
                "env": containerEnv,
                "volumeMounts": [
                  {
                    "name": "vol-%s" % [templateName],
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
                "image": "${ROUTER_IMAGE}",
                "env": containerEnv,
                "ports": [
                  {
                    "name": "amqp",
                    "containerPort": 5672
                  }
                ]
              }
            ],
            "containers": (if multicast == "true"
              then containers_common + [{
                  "name": "forwarder",
                  "env": containerEnv,
                  "image": "enmasseproject/topic-forwarder:latest" }]
              else containers_common)
          }
        }
      }
    }
  ],
  "parameters": [
    {
      "name": "BROKER_IMAGE",
      "description": "The image to use for the broker",
      "value": "enmasseproject/artemis:latest"
    },
    {
      "name": "ROUTER_IMAGE",
      "description": "The image to use for the router",
      "value": "gordons/qdrouterd:latest"
    },
    {
      "name": "ADDRESS",
      "description": "The address to use for the %s" % [addrtype],
      "required": true
    }
  ]
}
