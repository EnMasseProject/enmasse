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
  local internalPort = {
    "name": "internal",
    "port": 55673,
    "protocol": "TCP",
    "targetPort": 55673
  },
  generate(secure)::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "messaging",
        "annotations": {
          "service.alpha.openshift.io/infrastructure": "true",
          "service.alpha.openshift.io/dependencies": '[{"name":"configuration","namespace":"","kind":"Service"},{"name":"ragent","namespace":"","kind":"Service"},{"name":"subscription","namespace":"","kind":"Service"},{"name":"storage-controller","namespace":"","kind":"DeploymentConfig"}]'
        }
      },
      "spec": {
        "ports": if secure then [port, securePort, internalPort] else [port, internalPort],
        "selector": {
          "capability": "router"
        }
      }
    }
}
