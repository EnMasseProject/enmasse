local common = import "common.jsonnet";
local images = import "images.jsonnet";
{
  grafana_deployment::
  {
    "apiVersion": "extensions/v1beta1",
    "kind": "Deployment",
    "metadata": {
      "name": "grafana"
    },
    "spec": {
      "replicas": 1,
      "template": {
        "metadata": {
          "labels": {
            "name": "grafana"
          }
        },
        "spec": {
          "containers": [
            {
              "name": "grafana",
              "image": images.grafana,
              "ports": [
                common.container_port("grafana", 3000),
              ],
              "volumeMounts": [
                common.volume_mount("grafana-data", "/usr/share/grafana/data")
              ]
            }
          ],
          "volumes": [
            common.persistent_volume("grafana-data", "grafana-data")
          ]
        }
      }
    }
  },

  grafana_service::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": "grafana"
    },
    "spec": {
      "ports": [
        {
          "name": "grafana",
          "port": 3000,
          "targetPort": 3000 
        }
      ],
      "selector": {
        "name": "grafana"
      }
    }
  },

  grafana_pvc::
  {
    "apiVersion": "v1",
    "kind": "PersistentVolumeClaim",
    "metadata": {
      "name": "grafana-data"
    },
    "spec": {
      "accessModes": [
        "ReadWriteOnce"
      ],
      "resources": {
        "requests": {
          "storage": "1Gi"
        }
      }
    }
  },

  local me = self,

  grafana::
  {
    "apiVersion": "v1",
    "kind": "List",
    "items": [
      me.grafana_deployment,
      me.grafana_service,
      me.grafana_pvc
    ]
  }
}
