local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence, secure, router_image)::
    local addrtype = (if multicast then "topic" else "queue");
    local templateName = "%s%s-%s" % [if secure then "secure-" else "", addrtype, (if persistence then "persisted" else "inmemory")];
    { 
      "apiVersion": "v1",
      "kind": "Template",
      "metadata": {
        "name": templateName,
        "labels": {
          "addressType": addrtype
        }
      },
      "objects": [
        {
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
                "volumes": if secure
                  then [broker.volume(templateName), router.secret_volume()]
                  else [broker.volume(templateName)],

                "containers": if multicast
                  then [ broker.container(multicast, templateName), router.container("${COLOCATED_ROUTER_IMAGE}", secure), forwarder.container() ]
                  else [ broker.container(multicast, templateName), router.container("${COLOCATED_ROUTER_IMAGE}", secure) ]
              }
            }
          }
        }
      ],
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
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "required": true
        }
      ]
    }
}
