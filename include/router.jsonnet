local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("router", image_name),

  container(secure, addressEnv)::
    local routerPort = {
        "name": "amqp",
        "containerPort": 5672,
        "protocol": "TCP"
    };
    local brokerPort = {
        "name": "broker",
        "containerPort": 5673,
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
        then [routerPort, brokerPort, secureRouterPort] 
        else [routerPort, brokerPort],
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
