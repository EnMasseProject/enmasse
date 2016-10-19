{ 
  local port = {
    "name": "amqp",
    "port": 5672,
    "protocol": "TCP",
    "targetPort": 5672
  },
  local securePort = {
    "name": "amqps",
    "port": 5671,
    "protocol": "TCP",
    "targetPort": 5671
  },
  local brokerPort = {
    "name": "broker",
    "port": 5673,
    "protocol": "TCP",
    "targetPort": 5673
  },
  generate(secure)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "messaging"
      },
      "spec": {
        "ports": if secure then [port, securePort, brokerPort] else [port, brokerPort],
        "selector": {
          "capability": "router"
        }
      }
    }
}
