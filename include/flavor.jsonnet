{
  generate(secure)::
  local prefix = if secure then "tls-" else "";
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
      "json": "{\n    \"vanilla-queue\": { \"templateName\":\"" + prefix + "queue-inmemory\" },\n \"vanilla-topic\": { \"templateName\":\"" + prefix + "topic-inmemory\" }\n}\n"
    }
  }
}
