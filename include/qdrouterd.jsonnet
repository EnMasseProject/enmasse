local version = std.extVar("VERSION");
local router = import "router.jsonnet";
local common = import "common.jsonnet";
{ 
  deployment(secure)::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "qdrouterd",
          "app": "enmasse"
        },
        "name": "qdrouterd"
      },
      "spec": {
        "replicas": 1,
        "selector": {
          "name": "qdrouterd"
        },
        "triggers": [
          {
            "type": "ConfigChange"
          },
          common.trigger("router", "router")
        ],
        "template": {
          "metadata": {
            "labels": {
              "capability": "router",
              "name": "qdrouterd",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [ router.container(secure, "", "") ],
            [if secure then "volumes" ]: [
              router.secret_volume()
            ]
          }
        }
      }
    }
}
