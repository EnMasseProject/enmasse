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
              "type": "counter",
              "id": "${address}.${queue}.${broker}.messageCount",
              "description": "Message count for ${address}",
              "tags": {
                "messagingAddress": "${address}",
                "messagingBroker": "{broker}",
                "messagingQueue": "${queue}",
                "messagingMetricType": "messageCount"
              }
            },
            {
              "name": "org.apache.activemq.artemis:address=*,broker=*,component=addresses,queue=*,routing-type=*,subcomponent=queues#ConsumerCount",
              "type": "counter",
              "id": "${address}.${queue}.${broker}.consumerCount",
              "description": "Consumer count for ${address}",
              "tags": {
                "messagingAddress": "${address}",
                "messagingBroker": "{broker}",
                "messagingQueue": "${queue}",
                "messagingMetricType": "consumerCount"
              }
            }
          ]
        }
      ]
    })
  }
}
