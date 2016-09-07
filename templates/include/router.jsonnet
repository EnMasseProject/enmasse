{
  container(image_name, secure)::
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
      "image": image_name,
      "name": "router",
      "env": [
        {
          "name": "LINK_CAPACITY",
          "value": "${ROUTER_LINK_CAPACITY}"
      ],
      "ports": if secure
        then [routerPort, secureRouterPort] 
        else [routerPort],
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
