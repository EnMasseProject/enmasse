local version = std.extVar("VERSION");
local broker = import "broker.jsonnet";
local router = import "router.jsonnet";
local broker_repo = "${BROKER_REPO}";
local router_repo = "${ROUTER_REPO}";
local forwarder_repo = "${TOPIC_FORWARDER_REPO}";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence, secure)::
    local addrtype = (if multicast then "topic" else "queue");
    local addressEnv = (if multicast then { name: "TOPIC_NAME", value: "${ADDRESS}" } else { name: "QUEUE_NAME", value: "${ADDRESS}" });
    local volumeName = "vol-${TENANT}-${NAME}";
    local templateName = "%s%s-%s" % [if secure then "tls-" else "", addrtype, (if persistence then "persisted" else "inmemory")];
    local claimName = "pvc-${TENANT}-${NAME}";
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
          "name": "${TENANT}-${NAME}",
          "labels": {
            "app": "enmasse",
            "group_id": "${NAME}",
            "tenant": "${TENANT}",
            "address_config": "address-config-${TENANT}-${NAME}"
          }
        },
        "spec": {
          "replicas": 1,
          "template": {
            "metadata": {
              "labels": {
                "app": "enmasse",
                "role": "broker",
                "group_id": "${NAME}",
                "tenant": "${TENANT}"
              }
            },
            "spec": {
              local brokerVolume = if persistence
                then broker.persistedVolume(volumeName, claimName)
                else broker.volume(volumeName),
              "volumes": if secure
                then [brokerVolume, router.secret_volume()]
                else [brokerVolume],

              "containers": if multicast
                then [ broker.container(volumeName, broker_repo, addressEnv), router.container(secure, router_repo, addressEnv, "256Mi"), forwarder.container(forwarder_repo, addressEnv) ]
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
            "group_id": "${NAME}",
            "tenant": "${TENANT}",
            "app": "enmasse"
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
      local mcast = if multicast then "true" else "false",
      local config = {
        "apiVersion": "v1",
        "kind": "ConfigMap",
        "metadata": {
          "name": "address-config-${TENANT}-${NAME}",
          "labels": {
            "type": "address-config",
            "group_id": "${NAME}",
            "tenant": "${TENANT}",
            "app": "enmasse"
          }
        },
        "data": {
          "${ADDRESS}": "{\"store_and_forward\": true, \"multicast\": " + mcast + "}"
        }
      },
      "objects": if persistence
        then [pvc, controller, config]
        else [controller, config],
      "parameters": [
        {
          "name": "STORAGE_CAPACITY",
          "description": "Storage capacity required for volume claims",
          "value": "2Gi"
        },
        {
          "name": "BROKER_REPO",
          "description": "The docker image to use for the message broker",
          "value": "enmasseproject/artemis"
        },
        {
          "name": "TOPIC_FORWARDER_REPO",
          "description": "The default image to use as topic forwarder",
          "value": "enmasseproject/topic-forwarder"
        },
        {
          "name": "ROUTER_REPO",
          "description": "The image to use for the router",
          "value": "enmasseproject/qdrouterd"
        },
        {
          "name": "ROUTER_LINK_CAPACITY",
          "description": "The link capacity setting for router",
          "value": "50"
        },
        {
          "name": "TENANT",
          "description": "A valid tenant name for the instance",
          "required": true
        },
        {
          "name": "NAME",
          "description": "A valid name for the instance",
          "required": true
        },
        {
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "required": true
        }
      ]
    }
}
