local router = import "router.jsonnet";
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
  deployment(secure)::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
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
        "triggers": [
          {
            "type": "ConfigChange"
          },
          {
            "type": "ImageChange",
            "imageChangeParams": {
              "automatic": true,
              "containerNames": [
                "router"
              ],
              "from": {
                "kind": "ImageStreamTag",
                "name": "router:${ENMASSE_VERSION}"
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "capability": "router",
              "name": "qdrouterd"
            }
          },
          "spec": {
            "containers": [ router.container(secure, "") ],
            [if secure then "volumes" ]: [
              router.secret_volume()
            ]
          }
        }
      }
    }
}
