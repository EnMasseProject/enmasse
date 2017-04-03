local storage = import "storage-template.jsonnet";
local enmasseInfra = import "enmasse-instance-infra.jsonnet";
{
  generate(use_tls, use_sasl, compact, with_kafka, use_routes)::
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
      "queue-inmemory.json": std.toString(storage.template(false, false, false)),
      "tls-queue-inmemory.json": std.toString(storage.template(false, false, true)),
      "queue-persisted.json": std.toString(storage.template(false, true, false)),
      "tls-queue-persisted.json": std.toString(storage.template(false, true, true)),
      "topic-inmemory.json": std.toString(storage.template(true, false, false)),
      "tls-topic-inmemory.json": std.toString(storage.template(true, false, true)),
      "topic-persisted.json": std.toString(storage.template(true, true, false)),
      "tls-topic-persisted.json": std.toString(storage.template(true, true, true)),
      "enmasse-instance-infra.json": std.toString(enmasseInfra.generate(false, use_sasl, compact, with_kafka, use_routes)),
      "tls-enmasse-instance-infra.json": std.toString(enmasseInfra.generate(true, use_sasl, compact, with_kafka, use_routes))
    }
  }
}
