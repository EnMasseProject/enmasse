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
        "port": {
            "targetPort": "https"
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
      "tls": {
        "hosts": [
          hostname
        ],
        "secretName": ""
      },
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
