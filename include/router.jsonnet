local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("router", image_name),

  container(secure, addressEnv, mem_request)::
    local routerPort = {
        "name": "amqp",
        "containerPort": 5672,
        "protocol": "TCP"
    };
    local internalPort = {
        "name": "internal",
        "containerPort": 55673,
        "protocol": "TCP"
    };
    local secureRouterPort = {
        "name": "amqps",
        "containerPort": 5671,
        "protocol": "TCP"
    };
    local resources = {
        "requests": {
            "memory": mem_request,
        },
        "limits": {
            "memory": mem_request,
        }
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
        then [routerPort, internalPort, secureRouterPort] 
        else [routerPort, internalPort],
      "livenessProbe": {
        "tcpSocket": {
          "port": "amqp"
        }
      },
      [if mem_request != "" then "resources"]: resources,
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
