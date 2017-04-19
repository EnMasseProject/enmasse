local version = std.extVar("VERSION");
local router = import "router.jsonnet";
local common = import "common.jsonnet";
{
  deployment(use_tls, use_sasldb, instance, image_repo, collector_image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "qdrouterd",
          "app": "enmasse",
          "instance": instance
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
              "app": "enmasse",
              "instance": instance
            }
          },
          "spec": {
            "containers": [ router.container(use_tls, use_sasldb, image_repo, "", ""), router.collector(collector_image_repo, "32Mi") ],
            "volumes": [router.hawkular_volume()] +
              (if use_tls then [router.secret_volume()] else []) +
              (if use_sasldb then [router.sasldb_volume()] else [])
          }
        }
      }
    }
}
