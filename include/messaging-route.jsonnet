{
  generate(instance, hostname)::
  {
    "kind": "Route",
    "apiVersion": "v1",
    "metadata": {
        "labels": {
          "app": "enmasse",
          "instance": instance
        },
        "name": "messaging"
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
