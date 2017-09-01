local storage = import "storage-template.jsonnet";
local enmasseInfra = import "enmasse-instance-infra.jsonnet";
{
  generate(with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": "enmasse-template-config",
      "labels": {
        "app": "enmasse"
      }
    },
    "data": {
      "queue-inmemory.json": std.toString(storage.template(false, false)),
      "queue-persisted.json": std.toString(storage.template(false, true)),
      "topic-inmemory.json": std.toString(storage.template(true, false)),
      "topic-persisted.json": std.toString(storage.template(true, true)),
      "enmasse-instance-infra.json": std.toString(enmasseInfra.generate(with_kafka)),
    }
  }
}
