local router = import "router-common.jsonnet";
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
  generate(secure)::
  [
    {
      "apiVersion": "v1",
      "kind": "Service",
      "metadata": {
        "name": "messaging"
      },
      "spec": {
        "ports": if secure == "true" then [port, securePort] else [port],
        "selector": {
          "capability": "router"
        }
      }
    },
    {
      "apiVersion": "v1",
      "kind": "ReplicationController",
      "metadata": {
        "labels": {
          "name": "qdrouterd"
        },
        "name": "qdrouterd"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "qdrouterd"
        },
        "template": {
          "metadata": {
            "labels": {
              "capability": "router",
              "name": "qdrouterd"
            }
          },
          "spec": router.generate("${QDROUTER_IMAGE}", secure)
        }
      }
    }
  ]
}
