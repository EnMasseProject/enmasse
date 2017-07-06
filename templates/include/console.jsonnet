local router = import "router.jsonnet";
local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "console", "console", "http", 8080, 8080),

  container(use_sasldb, image_repo, env)::
    {
      local mount_path = "/var/lib/qdrouterd",
      local sasldb_env = [{
          "name": "SASLDB",
          "value": mount_path + "/qdrouterd.sasldb"
        }],
      "image": image_repo,
      "name": "console",
      "env": env + (if use_sasldb then sasldb_env else []),
      "resources": {
        "requests": {
          "memory": "64Mi",
        },
        "limits": {
          "memory": "64Mi",
        }
      },
      "ports": [
        {
          "name": "http",
          "containerPort": 8080,
          "protocol": "TCP"
        },
        {
          "name": "amqp-ws",
          "containerPort": 56720,
          "protocol": "TCP"
        }
      ],
      "livenessProbe": {
        "httpGet": {
          "port": "http",
          "path": "/"
        }
      },
      [if use_sasldb then "volumeMounts"]: [{"name": "sasldb-vol","mountPath": mount_path}]
   },

  deployment(use_sasldb, addressSpace, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "console",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "console"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "console",
              "app": "enmasse"
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [
              self.container(use_sasldb, image_repo, [{"name":"ADDRESS_SPACE_SERVICE_HOST","value":"${ADDRESS_SPACE_SERVICE_HOST}"}])
            ],
            [if use_sasldb then "volumes" ]: [router.sasldb_volume()]
          }
        }
      }
    },

  route(addressSpace, hostname)::
    {
      "kind": "Route",
      "apiVersion": "v1",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "console"
      },
      "spec": {
        "host": hostname,
        "to": {
          "kind": "Service",
          "name": "console"
        },
        "port": {
          "targetPort": "http"
        }
      }
    },

  ingress(addressSpace, hostname)::
    {
      "kind": "Ingress",
      "apiVersion": "extensions/v1beta1",
      "metadata": {
          "labels": {
            "app": "enmasse",
          },
          "annotations": {
            "addressSpace": addressSpace,
            "ingress.kubernetes.io/ssl-redirect": "false"
          },
          "name": "console"
      },
      "spec": {
        "rules": [
          {
            "host": hostname,
            "http": {
              "paths": [
                {
                  "path": "/",
                  "backend": {
                    "serviceName": "console",
                    "servicePort": 8080
                  }
                }
              ]
            }
          }
        ]
      }
    }
}
