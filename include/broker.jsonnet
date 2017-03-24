local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  container(secure, volumeName, image_repo, addressEnv)::
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
        },
        {
          "name": "jolokia",
          "containerPort": 8161 
        }
      ],
      "env": [ addressEnv, {"name": "GROUP_ID", "value": "${NAME}"} ],
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
