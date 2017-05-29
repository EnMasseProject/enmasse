{
  external::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "console-external",
        "labels": {
          "app": "enmasse"
        }
      },
      "spec": {
        "ports": [
          {
            "name": "console-ws",
            "port": 56720
          },
          {
            "name": "console-http",
            "port": 8080
          }
        ],
        "selector": {
          "name": "admin"
        },
        "type": "LoadBalancer"
      }
    }
}
