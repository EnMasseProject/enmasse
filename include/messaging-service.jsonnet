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
  generate(secure, instance, admin)::
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
      local full_deps = [
        {
          "name":"configuration",
          "namespace":"",
          "kind":"Service"
        },
        {
          "name":"ragent",
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
          "app": "enmasse",
          "instance": instance
        },
        "name": "messaging",
        "annotations": {
          "service.alpha.openshift.io/infrastructure": "true",
          "service.alpha.openshift.io/dependencies": if admin then std.toString(admin_deps) else std.toString(full_deps)
        }
      },
      "spec": {
        "ports": if secure then [port, securePort, internalPort, interRouterPort] else [port, internalPort, interRouterPort],
        "selector": {
          "capability": "router",
          "instance": instance
        }
      }
    }
}
