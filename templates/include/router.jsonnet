{
  imagestream(image_name)::
    {
      "apiVersion": "v1",
      "kind": "ImageStream",
      "metadata": {
        "name": "router"
      },
      "spec": {
        "dockerImageRepository": image_name,
        "tags": [
          {
            "name": "latest",
            "annotations": {
              "description": "Qpid Dispatch Router",
              "tags": "enmasse,messaging,router,qpid,amqp",
              "version": "1.0"
            }
          }
        ],
        "importPolicy": {
          "scheduled": true
        }
      }
    },

  container(secure, addressEnv)::
    local routerPort = {
        "name": "amqp",
        "containerPort": 5672,
        "protocol": "TCP"
    };
    local secureRouterPort = {
        "name": "amqps",
        "containerPort": 5671,
        "protocol": "TCP"
    };
    {
      "image": "router",
      "name": "router",
      local linkEnv = {
          "name": "LINK_CAPACITY",
          "value": "${ROUTER_LINK_CAPACITY}"
        },
      "env": if addressEnv == ""
        then [linkEnv]
        else [linkEnv, addressEnv],
      "ports": if secure
        then [routerPort, secureRouterPort] 
        else [routerPort],
      "livenessProbe": {
        "tcpSocket": {
          "port": "amqp"
        }
      },
      [if secure then "volumeMounts"]: [
        {
          "name": "ssl-certs",
          "mountPath": "/etc/qpid-dispatch/ssl",
          "readOnly": true
        }
      ]
    },

  secret_volume()::
    {
      "name": "ssl-certs",
      "secret": {
        "secretName": "qdrouterd-certs"
      }
    }
}
