{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "topic-forwarder"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": "${ENMASSE_VERSION}",
            "annotations": {
              "description": "Topic broker forwarder",
              "tags": "enmasse,messaging,broker,forwarding,amqp",
              "version": "1.0"
            }
          }
        ],
        "importPolicy": {
          "scheduled": true
        }
      }
    },
  container(addressEnv)::
    {
      "name": "forwarder",
      "env": [ addressEnv ],
      "image": "topic-forwarder:${ENMASSE_VERSION}",
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
