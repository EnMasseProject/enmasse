local storage = import "storage-template.jsonnet";
local standardInfra = import "standard-space-infra.jsonnet";
{
  generate(with_kafka)::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": "standard-template-config",
      "labels": {
        "app": "enmasse"
      }
    },
    "data": {
      "queue-inmemory.json": std.toString(storage.template(false, false)),
      "queue-persisted.json": std.toString(storage.template(false, true)),
      "topic-inmemory.json": std.toString(storage.template(true, false)),
      "topic-persisted.json": std.toString(storage.template(true, true)),
      "standard-space-infra.json": std.toString(standardInfra.generate(with_kafka)),
    }
  }
}
