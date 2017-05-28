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
  local interRouterPort = {
    "name": "inter-router",
    "port": 55672,
    "protocol": "TCP",
    "targetPort": 55672
  },
  generate(instance, type="ClusterIP")::
    {
      local admin_deps = [
        {
          "name":"admin",
          "namespace":"",
          "kind":"Service"
        },
        {
          "name":"subscription",
          "namespace":"",
          "kind":"Service"
        },
        {
          "name":"mqtt",
          "namespace":"",
          "kind":"Service"
        }
      ],
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "messaging",
        "annotations": {
          "instance": instance,
          "service.alpha.openshift.io/infrastructure": "true",
          "service.alpha.openshift.io/dependencies": std.toString(admin_deps)
        }
      },
      "spec": {
        "ports": [port, securePort, internalPort, interRouterPort],
        "selector": {
          "capability": "router"
        }
      },
      "type": type
    }
}
