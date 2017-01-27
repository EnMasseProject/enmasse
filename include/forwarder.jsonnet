local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("topic-forwarder", image_name),
  container(addressEnv)::
    {
      "name": "forwarder",
      "env": [ addressEnv ],
      "image": "topic-forwarder",
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
