local storage = import "storage-template.jsonnet";
local standardInfra = import "standard-space-infra.jsonnet";
local brokeredInfra = import "brokered-space-infra.jsonnet";
{
  global::
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
      "standard-space-infra.json": std.toString(standardInfra.template(true, true)),
      "standard-space-infra-without-mqtt.json": std.toString(standardInfra.template(true, false)),
      "brokered-space-infra.json": std.toString(brokeredInfra.template),
    }
  },

  storage(name)::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      }
    },
    "data": {
      "queue-inmemory.json": std.toString(storage.template(false, false)),
      "queue-persisted.json": std.toString(storage.template(false, true)),
      "topic-inmemory.json": std.toString(storage.template(true, false)),
      "topic-persisted.json": std.toString(storage.template(true, true))
    }
  }
}
