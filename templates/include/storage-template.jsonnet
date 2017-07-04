local images = import "images.jsonnet";
local broker = import "broker.jsonnet";
local router = import "router.jsonnet";
local broker_repo = "${BROKER_REPO}";
local router_repo = "${ROUTER_REPO}";
local forwarder_repo = "${TOPIC_FORWARDER_REPO}";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence)::
    local addrtype = (if multicast then "topic" else "queue");
    local addressEnv = (if multicast then { name: "TOPIC_NAME", value: "${ADDRESS}" } else { name: "QUEUE_NAME", value: "${ADDRESS}" });
    local volumeName = "vol-${NAME}";
    local templateName = "%s-%s" % [addrtype, (if persistence then "persisted" else "inmemory")];
    local claimName = "pvc-${NAME}";
    {
      "apiVersion": "v1",
      "kind": "Template",
      "metadata": {
        "name": templateName,
        "labels": {
          "app": "enmasse"
        }
      },

      local controller = {
        "apiVersion": "extensions/v1beta1",
        "kind": "Deployment",
        "metadata": {
          "name": "${NAME}",
          "labels": {
            "app": "enmasse"
          },
          "annotations": {
            "cluster_id": "${CLUSTER_ID}",
            "instance": "${INSTANCE}"
          }
        },
        "spec": {
          "replicas": 1,
          "template": {
            "metadata": {
              "labels": {
                "app": "enmasse",
                "role": "broker",
                "name": "${NAME}"
              },
              "annotations": {
                "cluster_id": "${CLUSTER_ID}",
                "instance": "${INSTANCE}"
              }
            },
            "spec": {
              local brokerVolume = if persistence
                then broker.persistedVolume(volumeName, claimName)
                else broker.volume(volumeName),
              "volumes": [brokerVolume, router.secret_volume("${COLOCATED_ROUTER_SECRET}"), broker.hawkularVolume()],

              "containers": if multicast
                then [ broker.container(volumeName, broker_repo, addressEnv),
                       router.container(false, router_repo, addressEnv, "256Mi"),
                       forwarder.container(forwarder_repo, addressEnv) ]
                else [ broker.container(volumeName, broker_repo, addressEnv) ]
            }
          }
        }
      },

      local pvc = {
        "apiVersion": "v1",
        "kind": "PersistentVolumeClaim",
        "metadata": {
          "name": claimName,
          "labels": {
            "app": "enmasse"
          },
          "annotations": {
            "cluster_id": "${CLUSTER_ID}",
            "instance": "${INSTANCE}",
          }
        },
        "spec": {
          "accessModes": [
            "ReadWriteMany"
          ],
          "resources": {
            "requests": {
              "storage": "${STORAGE_CAPACITY}"
            }
          }
        }
      },
      "objects": if persistence
        then [pvc, controller]
        else [controller],
      "parameters": [
        {
          "name": "STORAGE_CAPACITY",
          "description": "Storage capacity required for volume claims",
          "value": "2Gi"
        },
        {
          "name": "BROKER_REPO",
          "description": "The docker image to use for the message broker",
          "value": images.artemis
        },
        {
          "name": "TOPIC_FORWARDER_REPO",
          "description": "The default image to use as topic forwarder",
          "value": images.topic_forwarder
        },
        {
          "name": "ROUTER_REPO",
          "description": "The image to use for the router",
          "value": images.router
        },
        {
          "name": "ROUTER_LINK_CAPACITY",
          "description": "The link capacity setting for router",
          "value": "50"
        },
        {
          "name": "INSTANCE",
          "description": "A valid instance name for the instance",
          "required": true
        },
        {
          "name": "NAME",
          "description": "A valid name for the instance",
          "required": true
        },
        {
          "name": "CLUSTER_ID",
          "description": "A valid group id for the instance",
          "required": true
        },
        {
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "required": true
        },
        {
          "name": "COLOCATED_ROUTER_SECRET",
          "description": "Name of secret containing router key and certificate",
          "required": true
        }
      ]
    }
}
