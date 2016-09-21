local version = std.extVar("VERSION");
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
{
  template(multicast, persistence, secure)::
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
        "kind": "DeploymentConfig",
        "metadata": {
          "name": addrtype + "-${ADDRESS}"
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
                then [ broker.container(volumeName, addressEnv), router.container(secure, addressEnv), forwarder.container(addressEnv) ]
                else [ broker.container(volumeName, addressEnv), router.container(secure, addressEnv) ]
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
      local config = {
        "apiVersion": "v1",
        "kind": "ConfigMap",
        "metadata": {
          "name": "config-${ADDRESS}",
          "labels": {
            "type": "address-config"
          }
        },
        "data": {
          "address": "${ADDRESS}",
          "store-and-forward": "true",
          "multicast": if multicast then "true" else "false"
        }
      },
      "objects": if persistence
        then [pvc, controller, config]
        else [controller, config],
      "parameters": [
        {
          "name": "ROUTER_LINK_CAPACITY",
          "description": "The link capacity setting for router",
          "value": "50"
        },
        {
          "name": "STORAGE_CAPACITY",
          "description": "Storage capacity required for volume claims",
          "value": "2Gi"
        },
        {
          "name": "ADDRESS",
          "description": "The address to use for the %s" % [addrtype],
          "required": true
        }
      ]
    }
}
