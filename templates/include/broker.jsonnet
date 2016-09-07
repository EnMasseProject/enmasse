{
  container(multicast, volumeName)::
    local containerEnv = (if multicast then [ { name: "TOPIC_NAME", value: "${ADDRESS}" } ] else [ { name: "QUEUE_NAME", value: "${ADDRESS}" }]);
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
          "name": "vol-%s" % [volumeName],
          "mountPath": "/var/run/artemis"
        }
      ],
      [if multicast == false then "lifecycle"]: {
        "preStop": {
          "exec": {
            "command": [
              "/artemis-shutdown-hook/bin/artemis-shutdown-hook"
           ]
          }
        }
      }
    },

  volume(name)::
    {
      "name": "vol-%s" % [name],
      "emptyDir": {}
    }
}
