{
  generate(addressSpace, hostname)::
  {
    "kind": "Route",
    "apiVersion": "v1",
    "metadata": {
        "labels": {
          "app": "enmasse",
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "mqtt"
    },
    "spec": {
        "host": hostname,
        "to": {
            "kind": "Service",
            "name": "mqtt",
            "weight": 100
        },
        "port": {
            "targetPort": "secure-mqtt"
        },
        "tls": {
            "termination": "passthrough"
        }
    }
  }
}
