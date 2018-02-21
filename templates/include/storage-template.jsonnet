local images = import "images.jsonnet";
local broker = import "broker.jsonnet";
local common = import "common.jsonnet";
local router = import "router.jsonnet";
local broker_repo = "${BROKER_IMAGE}";
local router_repo = "${ROUTER_IMAGE}";
local forwarder_repo = "${TOPIC_FORWARDER_IMAGE}";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence)::
    local addrtype = (if multicast then "topic" else "queue");
    local addressEnv = (if multicast then { name: "TOPIC_NAME", value: "${ADDRESS}" } else { name: "QUEUE_NAME", value: "${ADDRESS}" });
    local volumeName = "${NAME}";
    local templateName = "%s-%s" % [addrtype, (if persistence then "persisted" else "inmemory")];
    {
      "apiVersion": "v1",
      "kind": "Template",
      "metadata": {
        "name": templateName,
        "labels": {
          "app": "enmasse"
        }
      },

      "objects": [
        {
          "apiVersion": "v1",
          "kind": "Service",
          "metadata": {
            "labels": {
              "app": "enmasse"
            },
            "name": "${NAME}",
            "annotations": {
              "cluster_id": "${CLUSTER_ID}",
              "addressSpace": "${ADDRESS_SPACE}"
            }
          },
          "spec": {
            "clusterIP": "None",
            "selector": {
              "role": "broker",
              "name": "${NAME}"
            }
          }
        },
        {
          "apiVersion": "apps/v1beta1",
          "kind": "StatefulSet",
          "metadata": {
            "name": "${NAME}",
            "labels": {
              "app": "enmasse"
            },
            "annotations": {
              "cluster_id": "${CLUSTER_ID}",
              "address": "${ADDRESS}",
              "addressSpace": "${ADDRESS_SPACE}",
              "io.enmasse.certCn": "broker",
              "io.enmasse.certSecretName" : "broker-internal-cert"
            }
          },
          "spec": {
            "serviceName": "${NAME}",
            "replicas": 1,
            "template": {
              "metadata": {
                "labels": {
                  "app": "enmasse",
                  "role": "broker",
                  "addresstype": addrtype,
                  "name": "${NAME}"
                },
                "annotations": {
                  "cluster_id": "${CLUSTER_ID}",
                  "address": "${ADDRESS}",
                  "addressSpace": "${ADDRESS_SPACE}",
                  "prometheus.io/scrape": "true",
                  "prometheus.io/path": "/metrics",
                  "prometheus.io/port": "8080"
                }
              },
              "spec": {
                "volumes": [
                  common.secret_volume("ssl-certs", "${COLOCATED_ROUTER_SECRET}"),
                  common.secret_volume("authservice-ca", "authservice-ca"),
                  common.secret_volume("address-controller-ca", "address-controller-ca"),
                  common.secret_volume("broker-internal-cert", "broker-internal-cert"),
                  common.configmap_volume("broker-prometheus-config", "broker-prometheus-config")
                ] + (if !persistence then [common.empty_volume(volumeName)] else []),

                "containers": if multicast
                  then [ broker.container(volumeName, broker_repo, addressEnv),
                         router.container(router_repo, [addressEnv], "256Mi", "broker-internal-cert"),
                         forwarder.container(forwarder_repo, addressEnv) ]
                  else [ broker.container(volumeName, broker_repo, addressEnv) ]
              }
            },
            [if persistence then "volumeClaimTemplates"]: [
              {
                "apiVersion": "v1",
                "kind": "PersistentVolumeClaim",
                "metadata": {
                  "name": volumeName,
                  "labels": {
                    "app": "enmasse"
                  },
                  "annotations": {
                    "cluster_id": "${CLUSTER_ID}",
                    "addressSpace": "${ADDRESS_SPACE}",
                  }
                },
                "spec": {
                  "accessModes": [
                    "ReadWriteOnce"
                  ],
                  "resources": {
                    "requests": {
                      "storage": "${STORAGE_CAPACITY}"
                    }
                  }
                }
              }
            ]
          }
        }
      ],
      "parameters": [
        {
          "name": "STORAGE_CAPACITY",
          "description": "Storage capacity required for volume claims",
          "value": "2Gi"
        },
        {
          "name": "BROKER_IMAGE",
          "description": "The docker image to use for the message broker",
          "value": images.artemis
        },
        {
          "name": "TOPIC_FORWARDER_IMAGE",
          "description": "The default image to use as topic forwarder",
          "value": images.topic_forwarder
        },
        {
          "name": "ROUTER_IMAGE",
          "description": "The image to use for the router",
          "value": images.router
        },
        {
          "name": "ROUTER_LINK_CAPACITY",
          "description": "The link capacity setting for router",
          "value": "50"
        },
        {
          "name": "ADDRESS_SPACE",
          "description": "A valid addressSpace name for the address Space",
          "required": true
        },
        {
          "name": "NAME",
          "description": "A valid name for the deployment",
          "required": true
        },
        {
          "name": "CLUSTER_ID",
          "description": "A valid group id for the deployment",
          "required": true
        },
        {
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "value": ""
        },
        {
          "name": "COLOCATED_ROUTER_SECRET",
          "description": "Name of secret containing router key and certificate",
          "required": true
        },
        {
          "name": "AUTHENTICATION_SERVICE_HOST",
          "description": "The hostname of the authentication service used by this address space",
          "required": true
        },
        {
          "name": "AUTHENTICATION_SERVICE_PORT",
          "description": "The port of the authentication service used by this address space",
          "required": true
        },
        {
          "name": "AUTHENTICATION_SERVICE_CA_SECRET",
          "description": "The secret to use for validating authentication service cert",
          "required": true
        },
        {
          "name": "AUTHENTICATION_SERVICE_CLIENT_SECRET",
          "description": "The client cert to use as identity against authentication service",
        },
        {
          "name": "AUTHENTICATION_SERVICE_SASL_INIT_HOST",
          "description": "The hostname to use in sasl init",
        },
        {
          "name": "BROKER_MEMORY_LIMIT",
          "description": "Memory limits for Artemis",
          "value": "512Mi"
        }
      ]
    }
}
