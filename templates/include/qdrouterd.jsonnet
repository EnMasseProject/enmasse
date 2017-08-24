local router = import "router.jsonnet";
local common = import "common.jsonnet";
{
  deployment(addressSpace, image_repo, metrics_image_repo, router_secret, auth_service_ca_secret)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "qdrouterd",
          "app": "enmasse",
          "io.enmasse.cert-secret-name" : "router-internal-cert"
        },
        "annotations": {
          "addressSpace": addressSpace
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
            },
            "annotations": {
              "addressSpace": addressSpace
            }
          },
          "spec": {
            "containers": [ router.container(image_repo, [], "", "router-internal-cert"),
              router.metrics(metrics_image_repo, "32Mi") ],
            "volumes": [
              router.hawkular_volume(),
              router.secret_volume("ssl-certs", router_secret),
              router.secret_volume("authservice-ca", auth_service_ca_secret),
              router.secret_volume("router-internal-cert", "router-internal-cert"),
            ]
          }
        }
      }
    }
}
