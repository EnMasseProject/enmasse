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
        "vanilla-queue": flavor.generate(secure, "queue-inmemory", null),
        "vanilla-topic": flavor.generate(secure, "topic-inmemory", null),
        "small-persisted-queue": flavor.generate(secure, "queue-persisted", "1Gi"),
        "large-persisted-queue": flavor.generate(secure, "queue-persisted", "10Gi"),
        "small-persisted-topic": flavor.generate(secure, "topic-persisted", "1Gi"),
        "large-persisted-topic": flavor.generate(secure, "topic-persisted", "10Gi"),
      },
      "json": std.toString(flavors),
    }
  }
}
