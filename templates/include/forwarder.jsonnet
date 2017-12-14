local common = import "common.jsonnet";
{
  container(image_repo, addressEnv)::
    {
      "name": "forwarder",
      "env": [
        addressEnv,
        {
          "name": "CLUSTER_ID",
          "value": "${NAME}"
        },
        {
          "name": "CERT_DIR",
          "value": "/etc/enmasse-certs"
        }
      ],
      "image": image_repo,
      "resources": {
          "requests": {
              "memory": "128Mi"
          },
          "limits": {
              "memory": "128Mi"
          }
      },
      "ports": [
        {
          "name": "health",
          "containerPort": 8088
        }
      ],
      "volumeMounts": [
        {
          "name": "broker-internal-cert",
          "mountPath": "/etc/enmasse-certs",
          "readOnly": true
        }
      ],
      "livenessProbe": {
        "httpGet": {
          "path": "/health",
          "port": "health"
        },
        "initialDelaySeconds": 60
      }
    }
}
