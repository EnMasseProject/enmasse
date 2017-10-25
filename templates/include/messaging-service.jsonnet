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
  local amqpsNormalPort = {
    "name": "amqps-normal",
    "port": 55671,
    "protocol": "TCP",
    "targetPort": "amqps-normal"
  },
  local amqpsBrokerPort = {
    "name": "amqps-broker",
    "port": 56671,
    "protocol": "TCP",
    "targetPort": "amqps-broker"
  },
  local interRouterPort = {
    "name": "inter-router",
    "port": 55672,
    "protocol": "TCP",
    "targetPort": 55672
  },

  internal(addressSpace)::
    {
      local admin_deps = [
        {
          "name":"queue-scheduler",
          "namespace":"",
          "kind":"Service"
        },
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
          "name":"console",
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
          "addressSpace": addressSpace,
          "io.enmasse.endpointPort": "amqps",
          "service.alpha.openshift.io/infrastructure": "true",
          "service.alpha.openshift.io/dependencies": std.toString(admin_deps)
        }
      },
      "spec": {
        "ports": [port, securePort, amqpsNormalPort, amqpsBrokerPort, interRouterPort],
        "selector": {
          "capability": "router"
        }
      }
    },

  external::
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "name": "messaging-external"
      },
      "spec": {
        "ports": [port, securePort],
        "selector": {
          "capability": "router"
        },
        "type": "LoadBalancer"
      }
    }
}
