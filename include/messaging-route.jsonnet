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
        "name": tenant + "-messaging"
    },
    "spec": {
        "host": hostname,
        "to": {
            "kind": "Service",
            "name": "messaging",
            "weight": 100
        },
        "port": {
            "targetPort": "amqps"
        },
        "tls": {
            "termination": "passthrough"
        }
    }
  }
}
