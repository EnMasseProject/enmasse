{
  imagestream(broker_image)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "artemis"
      },
      "spec": {
        "dockerImageRepository": broker_image,
        "tags": [
          {
            "name": "latest",
            "annotations": {
              "description": "Artemis message broker",
              "tags": "enmasse,messaging,activemq,artemis,broker,amqp",
              "version": "1.0"
            }
          }
        ],
        "importPolicy": {
          "scheduled": true
        }
      }
    },
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
