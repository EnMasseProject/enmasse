local router = import "router.jsonnet";
local authService = import "auth-service.jsonnet";
local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "console", "agent", "https", 8081, 8080),

  container(image_repo, env)::
    {
      "image": image_repo,
      "name": "agent",
      "env": env + authService.envVars,
      "resources": {
        "requests": {
          "memory": "128Mi",
        },
        "limits": {
          "memory": "128Mi",
        }
      },
      "ports": [
        common.container_port("https", 8080),
        common.container_port("amqp-ws", 56720),
      ],
      "livenessProbe": common.http_probe("https", "/probe", "HTTPS"),
      "readinessProbe": common.http_probe("https", "/probe", "HTTPS")
   },

  deployment(addressSpace, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "agent",
          "app": "enmasse"
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "agent"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "agent",
              "app": "enmasse"
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [
              self.container(image_repo, [{"name":"ADDRESS_SPACE_SERVICE_HOST","value":"${ADDRESS_SPACE_SERVICE_HOST}"}])
            ]
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
          "targetPort": "https"
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
            "https": {
              "paths": [
                {
                  "path": "/",
                  "backend": {
                    "serviceName": "console",
                    "servicePort": 8081
                  }
                }
              ]
            }
          }
        ]
      }
    }
}
