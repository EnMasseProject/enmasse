local common = import "common.jsonnet";
local authService = import "auth-service.jsonnet";
{
  metrics(image_repo, mem_request)::
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
          "port": "metrics"
        }
      },
      "ports": [
        {
          "name": "metrics",
          "containerPort": 8080,
          "protocol": "TCP"
        }
      ]

    },

  container(use_sasldb, image_repo, env, mem_request)::
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
      "image": image_repo,
      "name": "router",
      local linkEnv = [
        {
          "name": "LINK_CAPACITY",
          "value": "${ROUTER_LINK_CAPACITY}"
        }],
      "env": env + linkEnv + authService.envVars,

      "ports": [routerPort, internalPort, secureRouterPort],
      "livenessProbe": {
        "tcpSocket": {
          "port": "amqp"
        }
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

      local sasldb_vol = [{
          "name": "sasldb-vol",
          "mountPath": "/var/lib/qdrouterd"
        }],
      [if mem_request != "" then "resources"]: resources,
      "volumeMounts": ssl_certs + authservice_ca + (if use_sasldb then sasldb_vol else [])
    },

  secret_volume(name, secret)::
    {
      "name": name,
      "secret": {
        "secretName": secret
      }
    },

  hawkular_volume()::
    {
      "name": "hawkular-openshift-agent",
      "configMap": {
          "name": "hawkular-router-config"
      }
    },

  sasldb_volume()::
    {
      "name": "sasldb-vol",
       "persistentVolumeClaim": {
          "claimName": "pvc-sasldb"
        }
    },

  sasldb_pvc()::
    {
        "apiVersion": "v1",
        "kind": "PersistentVolumeClaim",
        "metadata": {
          "name": "pvc-sasldb",
        },
        "spec": {
          "accessModes": [
            "ReadWriteMany"
          ],
          "resources": {
            "requests": {
              "storage": "1Gi"
            }
          }
        }
    }
}
