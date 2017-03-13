local version = std.extVar("VERSION");
local mqtt = import "mqtt.jsonnet";
local common = import "common.jsonnet";
{
  deployment(secure, tenant, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "mqtt-gateway",
          "tenant": tenant,
          "app": "enmasse"
        },
        "name": tenant + "-mqtt-gateway"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "mqtt-gateway",
              "tenant": tenant,
              "app": "enmasse"
            }
          },
          "spec": {
            "containers":
            if secure
            then [ mqtt.container(true, image_repo), mqtt.container(false, image_repo) ]
            else [ mqtt.container(false, image_repo) ],
            [if secure then "volumes" ]: [
              mqtt.secret_volume()
            ]
          }
        }
      }
    }
}
