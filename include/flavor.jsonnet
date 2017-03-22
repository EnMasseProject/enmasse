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
        flavor.generate("vanilla-queue", "83fc2eaf-d968-4f7d-bbcd-da697ca9232c", secure, "queue", "Simple in memory queue", "queue-inmemory", null),
        flavor.generate("small-persisted-queue", "0d2a6a02-0ca2-4e8d-ad9b-a1f3319aba61", secure, "queue", "Small queue with persistence", "queue-persisted", "1Gi"),
        flavor.generate("large-persisted-queue", "ea4e1bf9-2501-43c0-bc47-0f0858fdbf89", secure, "queue", "Large queue with persistence", "queue-persisted", "10Gi"),
        flavor.generate("vanilla-topic", "2aa7214e-41b7-444a-a5f6-a73be3da9667", secure, "topic", "Simple in memory topic", "topic-inmemory", null),
        flavor.generate("small-persisted-topic", "c59e84f5-8f1b-4d17-a0d1-1b68085c96fc", secure, "topic", "Small topic with persistence", "topic-persisted", "1Gi"),
        flavor.generate("large-persisted-topic", "cbde8a2e-6690-46fa-9282-6876c38a469d", secure, "topic", "Large topic with persistence", "topic-persisted", "10Gi"),
      ],
      "json": std.toString(flavors),
    }
  }
}
