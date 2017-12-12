local common = import "common.jsonnet";
local authService = import "auth-service.jsonnet";
{
  metrics(image_repo, mem_request, internal_cert_volume)::
    {
      "image": image_repo,
      "name": "metrics",
      "resources": {
          "requests": {
              "memory": mem_request
          },
          "limits": {
              "memory": mem_request
          }
      },
      "livenessProbe": {
        "tcpSocket": {
          "port": "routermetrics"
        },
        "initialDelaySeconds": 60
      },
      "env": [
        {
          "name": "ROUTER_HOST",
          "value": "127.0.0.1"
        },
        {
          "name": "ROUTER_PORT",
          "value": "55671"
        },
        {
          "name": "CERT_DIR",
          "value": "/etc/enmasse-certs"
        }
      ],
      "ports": [
        {
          "name": "routermetrics",
          "containerPort": 8080,
          "protocol": "TCP"
        }
      ],
      local router_internal_cert = [{
        "name": internal_cert_volume,
        "mountPath": "/etc/enmasse-certs",
        "readOnly": true
      }],
      "volumeMounts": router_internal_cert
    },

  container(image_repo, env, mem_request, internal_cert_volume)::
    local routerPort = {
        "name": "amqp",
        "containerPort": 5672,
        "protocol": "TCP"
    };
    local amqpsNormalPort = {
        "name": "amqps-normal",
        "containerPort": 55671,
        "protocol": "TCP"
    };
    local amqpsBrokerPort = {
        "name": "amqps-broker",
        "containerPort": 56671,
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
      "image": image_repo,
      "name": "router",
      local linkEnv = [
        {
          "name": "LINK_CAPACITY",
          "value": "${ROUTER_LINK_CAPACITY}"
        }],
      "env": env + linkEnv + authService.envVars,

      "ports": [routerPort, secureRouterPort, amqpsNormalPort, amqpsBrokerPort],
      "livenessProbe": {
        "tcpSocket": {
          "port": "amqp"
        },
        "initialDelaySeconds": 60
      },
      local ssl_certs = [{
          "name": "ssl-certs",
          "mountPath": "/etc/qpid-dispatch/ssl",
          "readOnly": true
        }],

      local authservice_ca = [{
        "name": "authservice-ca",
        "mountPath": "/etc/qpid-dispatch/authservice-ca",
        "readOnly": true
      }],

      local router_internal_cert = [{
        "name": internal_cert_volume,
        "mountPath": "/etc/enmasse-certs",
        "readOnly": true
      }],

      [if mem_request != "" then "resources"]: resources,
      "volumeMounts": ssl_certs + authservice_ca + router_internal_cert
    },
}
