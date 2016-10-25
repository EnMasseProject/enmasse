local version = std.extVar("VERSION");
local router = import "router.jsonnet";
{ 
  deployment(secure)::
    {
      "apiVersion": "v1",
      "kind": "DeploymentConfig",
      "metadata": {
        "labels": {
          "name": "qdrouterd"
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
          {
            "type": "ImageChange",
            "imageChangeParams": {
              "automatic": true,
              "containerNames": [
                "router"
              ],
              "from": {
                "kind": "ImageStreamTag",
                "name": "router:" + version
              }
            }
          }
        ],
        "template": {
          "metadata": {
            "labels": {
              "capability": "router",
              "name": "qdrouterd"
            }
          },
          "spec": {
            "containers": [ router.container(secure, "") ],
            [if secure then "volumes" ]: [
              router.secret_volume()
            ]
          }
        }
      }
    }
}
