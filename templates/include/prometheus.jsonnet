
{
  broker_config(name)::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": name,
    },
    "data": {
      "config.yaml": std.toString({
        "lowercaseOutputName": true
      })
    }
  }
}
