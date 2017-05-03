local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  collector(image_repo, mem_request)::
    {
      "image": image_repo + ":" + version,
      "name": "collector",
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

  container(use_sasldb, image_repo, addressEnv, mem_request)::
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
      "image": image_repo + ":" + version,
      "name": "router",
      local linkEnv = {
          "name": "LINK_CAPACITY",
          "value": "${ROUTER_LINK_CAPACITY}"
        },
      "env": if addressEnv == ""
        then [linkEnv]
        else [linkEnv, addressEnv],
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
      local sasldb_vol = [{
          "name": "sasldb-vol",
          "mountPath": "/var/lib/qdrouterd"
        }],
      [if mem_request != "" then "resources"]: resources,
      "volumeMounts": ssl_certs + (if use_sasldb then sasldb_vol else [])
    },

  secret_volume(router_secret)::
    {
      "name": "ssl-certs",
      "secret": {
        "secretName": router_secret
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
