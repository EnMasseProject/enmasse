local metric = import "metric.jsonnet";
{
  "apiVersion": "v1",
  "kind": "ConfigMap",
  "metadata": {
    "name": "hawkular-router-config"
  },
  "data": {
    "hawkular-openshift-agent": std.toString({
      "endpoints": [
        {
          "type": "prometheus",
          "protocol": "http",
          "port": 8080,
          "path": "/metrics/",
          "collection_interval": "60s",
          "metrics": [
            metric.simple("totalConnectionCount"),
            metric.simple("totalLinkCount"),
            metric.simple("producerCount"),
            metric.simple("consumerCount"),
            //metric.simple("connectionCount"),
            //metric.simple("linkCount"),
            //metric.simple("addrCount"),
            //metric.simple("autoLinkCount"),
            //metric.simple("linkRouteCount"),
            //metric.simple("unsettledCount"),
            //metric.simple("deliveryCount"),
            metric.simple("releasedCount"),
            metric.simple("rejectedCount"),
            metric.simple("acceptedCount"),
            metric.simple("undeliveredCount"),
            //metric.simple("capacity")
          ]
        }
      ]
    })
  }
}
