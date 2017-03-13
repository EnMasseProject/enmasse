{
  local port = {
    "name": "amqp",
    "port": 5672,
    "protocol": "TCP",
    "targetPort": 5672
  },
  generate(secure, tenant)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "tenant": tenant
        },
        "name": "amqp-kafka-bridge"
      },
      "spec": {
        "ports": [port],
        "selector": {
          "capability": "bridge",
          "tenant": tenant
        }
      }
    }
}
