{
  standard_broker_config(name)::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": name,
    },
    "data": {
      "config.yaml": std.toString({
        "hostPort": "127.0.0.1:1099",
        "ssl": true,
        "rules": [
          {
            "pattern": "org.apache.activemq.artemis<broker=\"(.+)\", component=addresses, address=\"(.+)\".*><>MessageCount",
            "name": "artemis_message_count",
            "type": "GAUGE",
            "labels": {
              "broker": "$1",
              "address": "$2"
            }
          },
        ]
      })
    }
  },

  brokered_broker_config(name)::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": name,
    },
    "data": {
      "config.yaml": std.toString({
        "hostPort": "127.0.0.1:1099",
        "ssl": true,
        "rules": [
          {
            "pattern": "org.apache.activemq.artemis<broker=\"(.+)\"><>ConnectionCount",
            "name": "artemis_connection_count",
            "type": "GAUGE",
            "labels": {
              "broker": "$1"
            }
          },
          {
            "pattern": "org.apache.activemq.artemis<broker=\"(.+)\", component=addresses, address=\"(.+)\".*><>ConsumerCount",
            "name": "artemis_consumer_count",
            "type": "GAUGE",
            "labels": {
              "broker": "$1",
              "address": "$2"
            }
          },
          {
            "pattern": "org.apache.activemq.artemis<broker=\"(.+)\", component=addresses, address=\"(.+)\".*><>MessageCount",
            "name": "artemis_message_count",
            "type": "GAUGE",
            "labels": {
              "broker": "$1",
              "address": "$2"
            }
          },
        ]
      })
    }
  }
}
