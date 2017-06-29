local common = import "common.jsonnet";
{
  deployment(instance, image_repo)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "amqp-kafka-bridge",
          "app": "enmasse"
        },
        "annotations": {
          "instance": instance
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
            },
            "annotations": {
              "instance": instance
            }
          },
          "spec": {
            "containers": [
              common.clientContainer("amqp-kafka-bridge", image_repo, "512Mi", [
                        {
                          "name": "KAFKA_BOOTSTRAP_SERVERS",
                          "value": "${KAFKA_BOOTSTRAP_SERVERS}"
                        }], true),
            ]
          }
        }
      }
    }
}
