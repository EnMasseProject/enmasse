{
  container(volumeName, addressEnv)::
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
      "env": [ addressEnv ],
      "volumeMounts": [
        {
          "name": volumeName,
          "mountPath": "/var/run/artemis"
        }
      ],
      "lifecycle": {
        "preStop": {
          "exec": {
            "command": [
              "/opt/artemis-shutdown-hook/bin/artemis-shutdown-hook"
           ]
          }
        }
      }
    },

  volume(name)::
    {
      "name": name,
      "emptyDir": {}
    },

  persistedVolume(name, claimName)::
    {
      "name": name,
      "persistentVolumeClaim": {
          "claimName": claimName
      }
    }
}
