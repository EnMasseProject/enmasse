local common = import "common.jsonnet";
{
  container(image_repo, addressEnv)::
    {
      "name": "forwarder",
      "env": [ addressEnv, {"name": "CLUSTER_ID", "value": "${NAME}"} ],
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
          "containerPort": 8080
        }
      ],
      "livenessProbe": {
        "httpGet": {
          "path": "/health",
          "port": "health"
        }
      }
    }
}
