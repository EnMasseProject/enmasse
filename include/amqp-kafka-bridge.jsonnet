local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  deployment(image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "amqp-kafka-bridge",
          "app": "enmasse"
        },
        "name": "amqp-kafka-bridge"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "capability": "bridge",
              "name": "amqp-kafka-bridge",
              "app": "enmasse"
            }
          },
          "spec": {
            "containers": [
              common.containerWithEnv("amqp-kafka-bridge", image_repo, "amqp", 5672, [
                        {
                          "name": "KAFKA_BOOTSTRAP_SERVERS",
                          "value": "${KAFKA_BOOTSTRAP_SERVERS}"
                        }], "512Mi"),
            ]
          }
        }
      }
    }
}
