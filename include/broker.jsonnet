local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  container(volumeName, image_repo, addressEnv)::
    {
      "name": "broker",
      "image": image_repo + ":" + version,
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
      "env": [ addressEnv, {"name": "GROUP_ID", "value": "${NAME}"} ],
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
      "readinessProbe": {
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
