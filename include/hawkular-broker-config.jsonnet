{
  "apiVersion": "v1",
  "kind": "ConfigMap",
  "metadata": {
    "name": "hawkular-broker-config"
  },
  "data": {
    "hawkular-openshift-agent": std.toString({
      "endpoints": [
        {
          "type": "jolokia",
          "protocol": "http",
          "port": 8161,
          "path": "/jolokia/",
          "collection_interval": "60s",
          "metrics": [
            {
              "name": "java.lang:type=Threading#ThreadCount",
              "type": "counter",
              "id": "VM Thread Count"
            }
          ]
        }
      ]
    })
  }
}
