local router = import "router.jsonnet";
local common = import "common.jsonnet";
{
  service(instance)::
    common.service(instance, "console", "console", "http", 8080, 8080),

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

  deployment(use_sasldb, instance, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "console",
          "app": "enmasse"
        },
        "annotations": {
          "instance": instance
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
              "instance": instance
            }
          },
          "spec": {
            "containers": [
              self.container(use_sasldb, image_repo, [])
            ],
            [if use_sasldb then "volumes" ]: [router.sasldb_volume()]
          }
        }
      }
    },

  route(instance, hostname)::
    {
      "kind": "Route",
      "apiVersion": "v1",
      "metadata": {
        "labels": {
          "app": "enmasse"
        },
        "annotations": {
          "instance": instance
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
          "targetPort": "console-http"
        }
      }
    },

  ingress(instance, hostname)::
    {
      "kind": "Ingress",
      "apiVersion": "extensions/v1beta1",
      "metadata": {
          "labels": {
            "app": "enmasse",
          },
          "annotations": {
            "instance": instance
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
