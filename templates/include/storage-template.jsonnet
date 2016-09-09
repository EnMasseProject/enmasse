local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence, secure, router_image)::
    local addrtype = (if multicast then "topic" else "queue");
    local addressEnv = (if multicast then { name: "TOPIC_NAME", value: "${ADDRESS}" } else { name: "QUEUE_NAME", value: "${ADDRESS}" });
    local volumeName = "vol-${ADDRESS}";
    local templateName = "%s%s-%s" % [if secure then "secure-" else "", addrtype, (if persistence then "persisted" else "inmemory")];
    local claimName = "pvc-${ADDRESS}";
    { 
      "apiVersion": "v1",
      "kind": "Template",
      "metadata": {
        "name": templateName,
        "labels": {
          "addressType": addrtype
        }
      },
      local controller = {
        "apiVersion": "v1",
        "kind": "ReplicationController",
        "metadata": {
          "name": "controller-${ADDRESS}"
        },
        "spec": {
          "replicas": 1,
          "selector": {
            "address": "${ADDRESS}",
            "role": "broker"
          },
          "template": {
            "metadata": {
              "labels": {
                "role": "broker",
                "capability": "router",
                "address": "${ADDRESS}"
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
                then [ broker.container(volumeName, addressEnv), router.container("${COLOCATED_ROUTER_IMAGE}", secure, addressEnv), forwarder.container(addressEnv) ]
                else [ broker.container(volumeName, addressEnv), router.container("${COLOCATED_ROUTER_IMAGE}", secure, addressEnv) ]
            }
          }
        }
      },
      local pvc = {
        "apiVersion": "v1",
        "kind": "PersistentVolumeClaim",
        "metadata": {
          "name": claimName
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
          "name": "BROKER_IMAGE",
          "description": "The image to use for the broker",
          "value": "enmasseproject/artemis:latest"
        },
        {
          "name": "COLOCATED_ROUTER_IMAGE",
          "description": "The image to use for the router",
          "value": router_image
        },
        {
          "name": "TOPIC_FORWARDER_IMAGE",
          "description": "The image to use for the topic forwarder",
          "value": "enmasseproject/topic-forwarder:latest"
        },
        {
          "name": "ROUTER_LINK_CAPACITY",
          "description": "The link capacity setting for router",
          "value": "50"
        },
        {
          "name": "STORAGE_CAPACITY",
          "description": "Storage capacity required for volume claims",
          "value": "2Gi"
        }
        {
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "required": true
        }
      ]
    }
}
