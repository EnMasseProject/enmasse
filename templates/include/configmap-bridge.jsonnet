{
  generate(image_name)::
  {
    "apiVersion": "v1",
    "kind": "DeploymentConfig",
    "metadata": {
      "labels": {
        "name": "configmap-bridge"
      },
      "name": "configmap-bridge"
    },
    "spec": {
      "replicas": 1,
      "selector": {
        "name": "configmap-bridge"
      },
      "template": {
        "metadata": {
          "labels": {
            "name": "configmap-bridge"
          }
        },
        "spec": {
          "containers": [
            {
              "image": image_name,
              "name": "bridge",
              "ports": [
                {
                  "name": "amqp",
                  "containerPort": 5672
                }
              ]
            }
          ]
        }
      }
    }
  }
}
