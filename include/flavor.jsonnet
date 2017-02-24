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
      local flavors = {
        "vanilla-queue": flavor.generate(secure, "queue", "Simple in memory queue", "queue-inmemory", null),
        "vanilla-topic": flavor.generate(secure, "topic", "Simple in memory topic", "topic-inmemory", null),
        "small-persisted-queue": flavor.generate(secure, "queue", "Small queue with persistence", "queue-persisted", "1Gi"),
        "large-persisted-queue": flavor.generate(secure, "queue", "Large queue with persistence", "queue-persisted", "10Gi"),
        "small-persisted-topic": flavor.generate(secure, "topic", "Small topic with persistence", "topic-persisted", "1Gi"),
        "large-persisted-topic": flavor.generate(secure, "topic", "Large topic with persistence", "topic-persisted", "10Gi"),
      },
      "json": std.toString(flavors),
    }
  }
}
