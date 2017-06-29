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
              "id": "broker.threadCount",
              "tags": {
                "messagingComponent": "broker",
                "messagingMetricType": "threadCount"
              }
            },
            {
              "name": "java.lang:type=Memory#HeapMemoryUsage#used",
              "type": "gauge",
              "id": "broker.memoryHeapUsage",
              "tags": {
                "messagingComponent": "broker",
                "messagingMetricType": "heapUsage"
              }
            },
            {
              "name": "org.apache.activemq.artemis:address=*,broker=*,component=addresses,queue=*,routing-type=*,subcomponent=queues#MessageCount",
              "type": "gauge",
              "id": "${address}.${queue}.${broker}.queueDepth",
              "description": "Queue depth for ${address}",
              "tags": {
                "messagingAddress": "${address}",
                "messagingBroker": "{broker}",
                "messagingQueue": "${queue}",
                "messagingMetricType": "queueDepth"
              }
            },
            {
              "name": "org.apache.activemq.artemis:address=*,broker=*,component=addresses,queue=*,routing-type=*,subcomponent=queues#ConsumerCount",
              "type": "gauge",
              "id": "${address}.${queue}.${broker}.numConsumers",
              "description": "Number of consumers for ${address}",
              "tags": {
                "messagingAddress": "${address}",
                "messagingBroker": "{broker}",
                "messagingQueue": "${queue}",
                "messagingMetricType": "numConsumers"
              }
            }
          ]
        }
      ]
    })
  }
}
