local templateConfig = import "template-config.jsonnet";
local roles = import "roles.jsonnet";
local router = import "router.jsonnet";
local broker = import "broker.jsonnet";
local forwarder = import "forwarder.jsonnet";
local qdrouterd = import "qdrouterd.jsonnet";
local subserv = import "subserv.jsonnet";
local messagingService = import "messaging-service.jsonnet";
local messagingRoute = import "messaging-route.jsonnet";
local common = import "common.jsonnet";
local admin = import "admin.jsonnet";
local mqttGateway = import "mqtt-gateway.jsonnet";
local mqtt = import "mqtt.jsonnet";
local mqttService = import "mqtt-service.jsonnet";
local mqttRoute = import "mqtt-route.jsonnet";
local mqttLwt = import "mqtt-lwt.jsonnet";
local images = import "images.jsonnet";
local prometheus = import "prometheus.jsonnet";
local storage = import "storage-template.jsonnet";

{
  template(use_template_configmap, with_mqtt)::
  {
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": if (with_mqtt) then "standard-space-infra" else "standard-space-infra-without-mqtt",
    },

    local storage_templates = [
      storage.template(false, false),
      storage.template(false, true),
      storage.template(true, false),
      storage.template(true, true)
    ],

    local storage_template_configmap = [
      templateConfig.storage("enmasse-storage-templates")
    ],

    local template_config = (if use_template_configmap then "enmasse-storage-templates" else ""),

    local mqtt_components = [
      subserv.deployment("${ADDRESS_SPACE}", "${AGENT_IMAGE}"),
      mqttService.internal("${ADDRESS_SPACE}"),
      mqttGateway.deployment("${ADDRESS_SPACE}", "${MQTT_GATEWAY_IMAGE}", "${MQTT_SECRET}"),
      mqttLwt.deployment("${ADDRESS_SPACE}", "${MQTT_LWT_IMAGE}"),
    ],

    "objects": [
      messagingService.internal("${ADDRESS_SPACE}"),
      subserv.service("${ADDRESS_SPACE}"),
      prometheus.standard_broker_config("broker-prometheus-config"),
      qdrouterd.deployment("${ADDRESS_SPACE}", "${ROUTER_IMAGE}", "${ROUTER_METRICS_IMAGE}", "${MESSAGING_SECRET}", "authservice-ca"),
      common.ca_secret("authservice-ca", "${AUTHENTICATION_SERVICE_CA_CERT}"),
      admin.deployment("authservice-ca", template_config)
    ] + admin.services("${ADDRESS_SPACE}")
      + (if use_template_configmap then storage_template_configmap else storage_templates)
      + (if with_mqtt then mqtt_components else []),

    "parameters": [
      {
        "name": "ROUTER_IMAGE",
        "description": "The image to use for the router",
        "value": images.router
      },
      {
        "name": "ROUTER_METRICS_IMAGE",
        "description": "The image to use for the router metrics collector",
        "value": images.router_metrics
      },
      {
        "name": "ROUTER_LINK_CAPACITY",
        "description": "The link capacity setting for router",
        "value": "50"
      },
      {
        "name": "ALLOW_DURABLE_SUBSCRIPTIONS",
        "description": "Whether AMQP based durable subscriptions to topics are allowed",
        "value": "true"
      },
      {
        "name": "CONFIGSERV_IMAGE",
        "description": "The image to use for the configuration service",
        "value": images.configserv
      },
      {
        "name": "QUEUE_SCHEDULER_IMAGE",
        "description": "The container image to use for the queue scheduler",
        "value": images.queue_scheduler
      },
      {
        "name": "STANDARD_CONTROLLER_IMAGE",
        "description": "The container image to use for the standard controller",
        "value": images.standard_controller
      },
      {
        "name": "AGENT_IMAGE",
        "description": "The image to use for the enmasse agent",
        "value": images.agent
      },
      {
        "name": "MESSAGING_HOSTNAME",
        "description": "The hostname to use for the exposed route for messaging"
      },
      {
        "name" : "MQTT_GATEWAY_IMAGE",
        "description": "The image to use for the MQTT gateway",
        "value": images.mqtt_gateway
      },
      {
        "name": "MQTT_GATEWAY_HOSTNAME",
        "description": "The hostname to use for the exposed route for MQTT"
      },
      {
        "name": "CONSOLE_HOSTNAME",
        "description": "The hostname to use for the exposed route for the messaging console"
      },
      {
        "name": "CONSOLE_SECRET",
        "description": "The secret with cert for the console",
        "required": true
      },
      {
        "name": "MESSAGING_SECRET",
        "description": "The secret with cert for the messaging service",
        "required": true
      },
      {
        "name": "MQTT_SECRET",
        "description": "The secret to mount for MQTT private key and certificate",
        "required": true
      },
      {
        "name" : "MQTT_LWT_IMAGE",
        "description": "The image to use for the MQTT LWT",
        "value": images.mqtt_lwt
      },
      {
        "name": "ADDRESS_SPACE",
        "description": "The address space this infrastructure is deployed for",
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
        "name": "AUTHENTICATION_SERVICE_CA_CERT",
        "description": "The CA cert to use for validating authentication service cert",
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
        "name": "ENABLE_EVENT_LOGGER",
        "description": "Enable logging of kubernetes events",
        "value": "false"
      },
      {
        "name": "CONTROLLER_RESYNC_INTERVAL",
        "description": "Interval (in seconds) to use between controller resync",
        "value": "600"
      },
      {
        "name": "CONTROLLER_CHECK_INTERVAL",
        "description": "Interval (in seconds) to use between status checks",
        "value": "30"
      },
      {
        "name": "CONTROLLER_EVENT_QUEUE_SIZE",
        "description": "Max number of events queued up for controller",
        "value": "10000"
      },
      {
        "name": "ADDRESS_SPACE_ADMIN_SA",
        "description": "The service account with address space admin privileges",
        "value": "address-space-admin"
      }
    ]
  }
}
