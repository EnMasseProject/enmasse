{
  generate(image_name)::
  {
    "apiVersion": "v1",
    "kind": "DeploymentConfig",
    "metadata": {
      "labels": {
        "name": "ragent"
      },
      "name": "ragent"
    },
    "spec": {
      "replicas": 1,
      "selector": {
        "name": "ragent"
      },
      "template": {
        "metadata": {
          "labels": {
            "name": "ragent"
          }
        },
        "spec": {
          "containers": [
            {
              "image": image_name,
              "name": "master",
              "ports": [
                {
                  "containerPort": 55672,
                  "protocol": "TCP"
                }
              ]
            }
          ]
        }
      }
    }
  }
}
