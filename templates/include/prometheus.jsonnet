
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
        "lowercaseOutputName": true,
        "hostPort": "127.0.0.1:1099",
        "username": "admin",
        "password": "admin",
        "rules": [
          {
            "pattern": "org.apache.activemq.artemis<broker=\"(.+)\"><>ConnectionCount",
            "name": "connectionCount",
            "labels": {
              "broker": "$1"
            }
          }
        ]
      })
    }
  }
}
