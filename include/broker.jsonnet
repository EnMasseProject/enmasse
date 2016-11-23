local common = import "common.jsonnet";
{
  imagestream(broker_image)::
    common.imagestream("artemis", broker_image),

  container(volumeName, addressEnv)::
    {
      "name": "broker",
      "image": "artemis",
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
      "livenessProbe": {
        "tcpSocket": {
          "port": "amqp"
        }
      },
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
