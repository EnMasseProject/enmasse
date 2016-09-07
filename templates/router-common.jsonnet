{
  generate(image_name, secure)::
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
      "containers": [
        {
          "image": image_name,
          "name": "router",
          "ports": if secure == "true"
            then [routerPort, secureRouterPort] 
            else [routerPort],
          [if secure == "true" then "volumeMounts"]: [
            {
              "name": "ssl-certs",
              "mountPath": "/etc/qpid-dispatch/ssl",
              "readOnly": true
            }
          ]
        }
      ],
      [if secure == "true" then "volumes"]: [
        {
          "name": "ssl-certs",
          "secret": {
            "secretName": "qdrouterd-certs"
          }
        }
      ]
    }
}
