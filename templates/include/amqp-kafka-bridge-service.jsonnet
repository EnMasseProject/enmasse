{
  local port = {
    "name": "amqp",
    "port": 5672,
    "protocol": "TCP",
    "targetPort": 5672
  },
  generate(addressSpace)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse",
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "amqp-kafka-bridge"
      },
      "spec": {
        "ports": [port],
        "selector": {
          "capability": "bridge"
        }
      }
    }
}
