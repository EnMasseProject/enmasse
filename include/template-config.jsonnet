local storage = import "storage-template.jsonnet";
local enmasseInfra = import "enmasse-instance-infra.jsonnet";
{
  generate(use_sasl, with_kafka, use_routes)::
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
      "queue-persisted.json": std.toString(storage.template(true, false)),
      "topic-inmemory.json": std.toString(storage.template(false, false)),
      "topic-persisted.json": std.toString(storage.template(true, false)),
      "enmasse-instance-infra.json": std.toString(enmasseInfra.generate(use_sasl, with_kafka, use_routes)),
    }
  }
}
