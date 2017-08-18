local common = import "common.jsonnet";
{
  common_service(name, type, annotations)::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      },
      "annotations": annotations
    },
    "spec": {
      "ports": [
        {
          "name": "http",
          "port": 8080,
          "protocol": "TCP",
          "targetPort": "http"
        },
        {
          "name": "https",
          "port": 8081,
          "protocol": "TCP",
          "targetPort": "https"
        },
        {
          "name": "amqp",
          "port": 5672,
          "protocol": "TCP",
          "targetPort": "amqp"
        }
      ],
      "selector": {
        "name": "address-controller"
      },
      "type": type
    }
  },
  
  internal_service::
    self.common_service("address-controller", "ClusterIP", {"service.alpha.openshift.io/serving-cert-secret-name": "address-controller-certs"}),

  external_service::
    self.common_service("address-controller-external", "LoadBalancer", {}),

  deployment(image_repo, multiinstance, template_config)::
    {
      "apiVersion": "extensions/v1beta1",
      "kind": "Deployment",
      "metadata": {
        "labels": {
          "name": "address-controller",
          "app": "enmasse"
        },
        "name": "address-controller"
      },
      "spec": {
        "replicas": 1,
        "template": {
          "metadata": {
            "labels": {
              "name": "address-controller",
              "app": "enmasse"
            }
          },

          local template_mount = [{
              "name": "templates",
              "mountPath": "/enmasse-templates"
          }],

          local ssl_certs = [{
            "name": "ssl-certs",
            "mountPath": "/ssl-certs",
            "readOnly": true
          }],

          local mounts = if template_config != ""
            then template_mount + ssl_certs
            else ssl_certs,

          local ports = [
            {
              "name": "http",
              "containerPort": 8080,
              "protocol": "TCP"
            },
            {
              "name": "https",
              "containerPort": 8081,
              "protocol": "TCP"
            },
            {
              "name": "amqp",
              "containerPort": 5672,
              "protocol": "TCP"
            }
          ],
          "spec": {
            "serviceAccount": "enmasse-service-account",
            "containers": [
              {
                "image": image_repo,
                "name": "address-controller",
                "env": [{
                  "name": "MULTIINSTANCE",
                  "value": multiinstance
                }],
                "volumeMounts": mounts,
                "resources": {
                    "requests": {
                        "memory": "256Mi",
                    },
                    "limits": {
                        "memory": "256Mi",
                    }
                },
                "ports": ports,
                "livenessProbe": {
                  "tcpSocket": {
                    "port": "http"
                  }
                }
              }
            ],
            local template_volume = [{
                "name": "templates",
                "configMap": {
                  "name": template_config
                }
            }],

            local secret_volume = [{
                "name": "ssl-certs",
                "secret": {
                  "secretName": "address-controller-certs"
                }
            }],

            "volumes": if template_config != ""
              then template_volume + secret_volume
              else secret_volume
          }
        }
      }
    }
}
