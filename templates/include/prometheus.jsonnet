local common = import "common.jsonnet";
local images = import "images.jsonnet";

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
  },

  prometheus_deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": "prometheus"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "prometheus"
          }
        },
        "spec": {
          "serviceAccount": "prometheus-server",
          "containers": [
            {
              "name": "prometheus",
              "image": images.prometheus,
              "ports": [
                common.container_port("prometheus", 9090),
              ],
              "volumeMounts": [
                common.volume_mount("prometheus-config", "/etc/prometheus", true),
                common.volume_mount("prometheus-data", "/prometheus")
              ]
            }
          ],
          "volumes": [
            common.configmap_volume("prometheus-config", "prometheus-config"),
            common.persistent_volume("prometheus-data", "prometheus-data")
          ]
        }
      }
    }
  },

  prometheus_sa::
  {
    "apiVersion": "v1",
    "kind": "ServiceAccount",
    "metadata": {
      "name": "prometheus-server"
    }
  },

  prometheus_config::
  {
    "apiVersion": "v1",
    "kind": "ConfigMap",
    "metadata": {
      "name": "prometheus-config"
    },
    "data": {
      "prometheus.yml": std.toString(
        {
          "global": {
            "scrape_interval": "60s",
            "evaluation_interval": "60s"
          },
          "scrape_configs": [
            {
              "job_name": "artemis_job",
              "kubernetes_sd_configs": [
                {
                  "role": "pod"
                }
              ],
              "relabel_configs": [
                {
                  "source_labels": [
                    "__meta_kubernetes_pod_container_port_name"
                  ],
                  "action": "keep",
                  "regex": "artemismetrics.*"
                },
                {
                  "source_labels": [
                    "__meta_kubernetes_pod_name"
                  ],
                  "action": "replace",
                  "target_label": "kubernetes_pod_name"
                },
              ]
            },
            {
              "job_name": "router_job",
              "kubernetes_sd_configs": [
                {
                  "role": "pod"
                }
              ],
              "relabel_configs": [
                {
                  "source_labels": [
                    "__meta_kubernetes_pod_container_port_name"
                  ],
                  "action": "keep",
                  "regex": "routermetrics.*"
                },
                {
                  "source_labels": [
                    "__meta_kubernetes_pod_name"
                  ],
                  "action": "replace",
                  "target_label": "kubernetes_pod_name"
                },
              ]
            }
          ]
        })
    }
  },

  prometheus_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "prometheus"
    },
    "spec": {
      "ports": [
        {
          "name": "prometheus",
          "port": 9090,
          "targetPort": 9090
        }
      ],
      "selector": {
        "name": "prometheus"
      }
    }
  },

  prometheus_pvc::
  {
    "apiVersion": "v1",
    "kind": "PersistentVolumeClaim",
    "metadata": {
      "name": "prometheus-data"
    },
    "spec": {
      "accessModes": [
        "ReadWriteOnce"
      ],
      "resources": {
        "requests": {
          "storage": "10Gi"
        }
      }
    }
  },

  local me = self,

  prometheus::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      me.prometheus_deployment,
      me.prometheus_config,
      me.prometheus_service,
      me.prometheus_pvc,
      me.prometheus_sa
    ]
  }
}
