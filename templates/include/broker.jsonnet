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
          "name": "jolokia",
          "containerPort": 8161 
        }
      ],
      "env": [ addressEnv, {"name": "PN_TRACE_FRM", "value": "1"}, {"name": "CLUSTER_ID", "value": "${CLUSTER_ID}"}, {"name": "CERT_DIR", "value": "/etc/enmasse-certs"} ],
      "volumeMounts": [
        {
          "name": volumeName,
          "mountPath": "/var/run/artemis"
        },
        {
          "name": "broker-internal-cert",
          "mountPath": "/etc/enmasse-certs",
          "readOnly": true
        }
      ],
      "livenessProbe": {
        "tcpSocket": {
          "port": "amqp"
        },
        "initialDelaySeconds": 120
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
    }
}
