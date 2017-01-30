local version = std.extVar("VERSION");
local broker = import "broker.jsonnet";
local router = import "router.jsonnet";
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
        "apiVersion": "v1",
        "kind": "DeploymentConfig",
        "metadata": {
          "name": "${NAME}",
          "labels": {
            "app": "enmasse",
            "address_config": "address-config-${NAME}"
          }
        },
        "spec": {
          "replicas": 1,
          "selector": {
            "address": "${ADDRESS}",
            "role": "broker"
          },
          local commonTriggers = [
            {
              "type": "ConfigChange"
            },
            {
              "type": "ImageChange",
              "imageChangeParams": {
                "automatic": true,
                "containerNames": [
                  "broker"
                ],
                "from": {
                  "kind": "ImageStreamTag",
                  "name": "artemis:" + version
                }
              }
            }
          ],
          local multicastTriggers = [
            {
              "type": "ImageChange",
              "imageChangeParams": {
                "automatic": true,
                "containerNames": [
                  "router"
                ],
                "from": {
                  "kind": "ImageStreamTag",
                  "name": "router:" + version
                }
              }
            },
            {
              "type": "ImageChange",
              "imageChangeParams": {
                "automatic": true,
                "containerNames": [
                  "forwarder"
                ],
                "from": {
                  "kind": "ImageStreamTag",
                  "name": "topic-forwarder:" + version
                }
              }
            }
          ],
          "triggers": if multicast
            then commonTriggers + multicastTriggers
            else commonTriggers,
          "template": {
            "metadata": {
              "labels": {
                "app": "enmasse",
                "role": "broker",
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
                then [ broker.container(volumeName, addressEnv), router.container(secure, addressEnv, "256Mi"), forwarder.container(addressEnv) ]
                else [ broker.container(volumeName, addressEnv) ]
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
          "name": "address-config-${NAME}",
          "labels": {
            "type": "address-config",
            "group_id": "${NAME}",
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
          "name": "ROUTER_LINK_CAPACITY",
          "description": "The link capacity setting for router",
          "value": "50"
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
