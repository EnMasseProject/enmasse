local version = std.extVar("VERSION");
local router = import "router.jsonnet";
local common = import "common.jsonnet";
{ 
  deployment(secure, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "qdrouterd",
          "app": "enmasse"
        },
        "name": "qdrouterd"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "capability": "router",
              "name": "qdrouterd",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [ router.container(secure, image_repo, "", "") ],
            [if secure then "volumes" ]: [
              router.secret_volume()
            ]
          }
        }
      }
    }
}
