local flavor = import "flavorgen.jsonnet";
{
  generate(secure)::
  {
    "kind": "ConfigMap",
    "apiVersion": "v1",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": "flavor"
    },
    "data": {
      local flavors = [
        flavor.generate("vanilla-queue", secure, "queue", "Simple in memory queue", "queue-inmemory", null),
        flavor.generate("small-persisted-queue", secure, "queue", "Small queue with persistence", "queue-persisted", "1Gi"),
        flavor.generate("large-persisted-queue", secure, "queue", "Large queue with persistence", "queue-persisted", "10Gi"),
        flavor.generate("vanilla-topic", secure, "topic", "Simple in memory topic", "topic-inmemory", null),
        flavor.generate("small-persisted-topic", secure, "topic", "Small topic with persistence", "topic-persisted", "1Gi"),
        flavor.generate("large-persisted-topic", secure, "topic", "Large topic with persistence", "topic-persisted", "10Gi"),
      ],
      "json": std.toString(flavors),
    }
  }
}
