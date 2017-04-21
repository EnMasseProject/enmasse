local version = std.extVar("VERSION");
local broker = import "broker.jsonnet";
local router = import "router.jsonnet";
local broker_repo = "${BROKER_REPO}";
local router_repo = "${ROUTER_REPO}";
local router_collector_repo = "${ROUTER_COLLECTOR_REPO}";
local forwarder_repo = "${TOPIC_FORWARDER_REPO}";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence, secure)::
    local addrtype = (if multicast then "topic" else "queue");
    local addressEnv = (if multicast then { name: "TOPIC_NAME", value: "${ADDRESS}" } else { name: "QUEUE_NAME", value: "${ADDRESS}" });
    local volumeName = "vol-${NAME}";
    local templateName = "%s%s-%s" % [if secure then "tls-" else "", addrtype, (if persistence then "persisted" else "inmemory")];
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
            "app": "enmasse",
            "group_id": "${NAME}",
            "instance": "${INSTANCE}",
            "address_config": "address-config-${INSTANCE}-${NAME}"
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
                "instance": "${INSTANCE}"
              }
            },
            "spec": {
              local brokerVolume = if persistence
                then broker.persistedVolume(volumeName, claimName)
                else broker.volume(volumeName),
              "volumes": if secure
                then [brokerVolume, router.secret_volume(), broker.hawkularVolume()]
                else [brokerVolume, broker.hawkularVolume()],

              "containers": if multicast
                then [ broker.container(secure, volumeName, broker_repo, addressEnv),
                       router.container(secure, false, router_repo, addressEnv, "256Mi"),
                       forwarder.container(forwarder_repo, addressEnv) ]
                else [ broker.container(secure, volumeName, broker_repo, addressEnv) ]
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
            "instance": "${INSTANCE}",
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
          "value": "enmasseproject/artemis"
        },
        {
          "name": "ROUTER_COLLECTOR_REPO",
          "description": "The docker image to use for the router collector",
          "value": "enmasseproject/router-collector"
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
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "required": true
        }
      ]
    }
}
