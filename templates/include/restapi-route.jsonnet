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
        "host": hostname,
        "path": "/v1",
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
                "path": "/v1",
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
