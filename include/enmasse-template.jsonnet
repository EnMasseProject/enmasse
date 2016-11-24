local storage = import "storage-template.jsonnet";
local configserv = import "configserv.jsonnet";
local ragent = import "ragent.jsonnet";
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local storageController = import "storage-controller.jsonnet";
local subserv = import "subserv.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local messagingRoute = import "messaging-route.json";
local addressConfig = import "addresses.json";
local flavorConfig = import "flavor.jsonnet";
local common = import "common.jsonnet";
local restapi = import "restapi.jsonnet";
{
  generate(secure, with_storage_controller)::
  {
    local templateName = (if secure then "tls-enmasse" else "enmasse") + (if with_storage_controller then "" else "-base"),
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": templateName
    },
    local common = [ storage.template(false, false, secure),
                 storage.template(false, true, secure),
                 storage.template(true, false, secure),
                 storage.template(true, true, secure),
                 import "direct-template.json",

                 router.imagestream("${ROUTER_REPO}"),
                 qdrouterd.deployment(secure),
                 broker.imagestream("${BROKER_REPO}"),
                 forwarder.imagestream("${TOPIC_FORWARDER_REPO}"),
                 messagingService.generate(secure),
                 configserv.imagestream("${CONFIGSERV_REPO}"),
                 configserv.deployment,
                 configserv.service,
                 ragent.imagestream("${RAGENT_REPO}"),
                 ragent.deployment,
                 ragent.service,
                 subserv.imagestream("${SUBSERV_REPO}"),
                 subserv.deployment,
                 subserv.service ],
    local storage_controller_resources = [
                 storageController.imagestream("${STORAGE_CONTROLLER_REPO}"),
                 storageController.deployment,
                 storageController.service,
                 restapi.imagestream("${RESTAPI_REPO}"),
                 restapi.deployment,
                 restapi.service,
                 flavorConfig.generate(secure)],

    local all_objects = if with_storage_controller then common + storage_controller_resources else common,
    local secured_objects = all_objects + [ messagingRoute ],
    "objects": if secure then secured_objects else all_objects,
    "parameters": [
      {
        "name": "ROUTER_REPO",
        "description": "The image to use for the router",
        "value": "enmasseproject/qdrouterd"
      },
      {
        "name": "BROKER_REPO",
        "description": "The default image to use as broker",
        "value": "enmasseproject/artemis"
      },
      {
        "name": "TOPIC_FORWARDER_REPO",
        "description": "The default image to use as topic forwarder",
        "value": "enmasseproject/topic-forwarder"
      },
      {
        "name": "ROUTER_LINK_CAPACITY",
        "description": "The link capacity setting for router",
        "value": "50"
      },
      {
        "name": "CONFIGSERV_REPO",
        "description": "The image to use for the configuration service",
        "value": "enmasseproject/configserv"
      },
      {
        "name": "STORAGE_CONTROLLER_REPO",
        "description": "The docker image to use for the storage controller",
        "value": "enmasseproject/storage-controller"
      },
      {
        "name": "RAGENT_REPO",
        "description": "The image to use for the router agent",
        "value": "enmasseproject/ragent"
      },
      {
        "name": "SUBSERV_REPO",
        "description": "The image to use for the subscription services",
        "value": "enmasseproject/subserv"
      },
      {
        "name": "RESTAPI_REPO",
        "description": "The image to use for the rest api",
        "value": "enmasseproject/enmasse-rest"
      },
      {
        "name": "MESSAGING_HOSTNAME",
        "description": "The hostname to use for the exposed route for (TLS only)",
      }
    ]

  }
}
