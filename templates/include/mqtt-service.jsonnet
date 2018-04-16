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
  common(name, type, annotations)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "annotations": annotations,
        "name": name
      },
      "spec": {
        "ports": [port, securePort],
        "selector": {
          "name": "mqtt-gateway"
        },
        "type": type
      }
    },

  internal(addressSpace)::
    self.common("mqtt", "ClusterIP", {"addressSpace": addressSpace, "io.enmasse.endpointPort": "secure-mqtt", "enmasse.io/service-port.mqtt": 1883, "enmasse.io/service-port.mqtts": 8883}),

  external::
    self.common("mqtt-external", "LoadBalancer", {})
}
