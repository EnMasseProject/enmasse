{
  generate(image_name)::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "storage-controller"
        },
        "name": "storage-controller"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "storage-controller"
        },
        "template": {
          "metadata": {
            "labels": {
              "name": "storage-controller"
            }
          },
          "spec": {
            "serviceAccount": "deployer",
            "containers": [
              {
                "image": image_name,
                "name": "controller"
              }
            ]
          }
        }
      }
    }
}
