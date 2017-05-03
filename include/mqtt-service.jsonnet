{
  local port = {
    "name": "mqtt",
    "port": 1883,
    "protocol": "TCP",
    "targetPort": 1883
  },
  local securePort = {
    "name": "secure-mqtt",
    "port": 8883,
    "protocol": "TCP",
    "targetPort": 8883
  },
  generate(instance)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "instance": instance
        },
        "name": "mqtt"
      },
      "spec": {
        "ports": [port, securePort],
        "selector": {
          "name": "mqtt-gateway",
          "instance": instance
        }
      }
    }
}
