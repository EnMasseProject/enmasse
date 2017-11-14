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
      "to": {
        "kind": "Service",
        "name": "address-controller"
      },
      "tls": {
        "termination": "passthrough"
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
                "path": "/apis/enmasse.io/v1",
                "backend": {
                  "serviceName": "address-controller",
                  "servicePort": 443
                }
              }
            ]
          }
        }
      ]
    }
  }
}
