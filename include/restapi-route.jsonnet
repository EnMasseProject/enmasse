{
  route(hostname)::
  {
    "kind": "Route",
    "apiVersion": "v1",
    "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "restapi"
    },
    "spec": {
        "path": "/v3",
        "host": hostname,
        "to": {
            "kind": "Service",
            "name": "address-controller"
        },
        "port": {
            "targetPort": "http"
        }
    }
  },

  ingress(hostname)::
  {
    "kind": "Ingress",
    "apiVersion": "extensions/v1beta1",
    "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "restapi"
    },
    "spec": {
      "rules": [
        {
          "host": hostname,
          "http": {
            "paths": [
              {
                "path": "/v3",
                "backend": {
                  "serviceName": "address-controller",
                  "servicePort": 8080
                }
              }
            ]
          }
        }
      ]
    }
  }
}
