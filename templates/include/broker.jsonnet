local common = import "common.jsonnet";
{
  container(volumeName, image_repo, addressEnv)::
    {
      "name": "broker",
      "image": image_repo,
      "ports": [
        {
          "name": "amqp",
          "containerPort": 5673
        },
        {
          "name": "core",
          "containerPort": 61616
        },
        {
          "name": "jolokia",
          "containerPort": 8161 
        }
      ],
      "env": [ addressEnv, {"name": "GROUP_ID", "value": "${GROUP_ID}"} ],
      "volumeMounts": [
        {
          "name": volumeName,
          "mountPath": "/var/run/artemis"
        }
      ],
      //+ if secure then [
      //  {
      //    "name": "ssl-certs",
      //    "mountPath": "/etc/artemis/certs",
      //    "readOnly": true
      //  }
      //] else [],
      "livenessProbe": { "tcpSocket": {
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
    },

  hawkularVolume()::
    {
      "name": "hawkular-openshift-agent",
      "configMap": {
          "name": "hawkular-broker-config"
      }
    },

  secretVolume()::
    {
      "name": "ssl-certs",
      "secret": {
        "secretName": "broker-certs"
      }
    }
}
