{
  generate(tenant, hostname)::
  {
    "kind": "Route",
    "apiVersion": "v1",
    "metadata": {
        "labels": {
          "app": "enmasse",
          "tenant": tenant
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
