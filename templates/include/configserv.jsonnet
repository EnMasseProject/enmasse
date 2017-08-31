local common = import "common.jsonnet";
{
  service(addressSpace)::
    common.service(addressSpace, "configuration", "configserv", "amqps", 5671, 5671),
  deployment(addressSpace, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "app": "enmasse",
          "name": "configserv"
        },
        "annotations": {
          "addressSpace": addressSpace
        },
        "name": "configserv"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "app": "enmasse",
              "name": "configserv"
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [ common.container("configserv", image_repo, "amqps", 5671, "128Mi", []) ]
          }
        }
      }
    }
}
