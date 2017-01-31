local configserv = import "include/configserv.jsonnet";
local ragent = import "include/ragent.jsonnet";
local router = import "include/router.jsonnet";
local broker = import "include/broker.jsonnet";
local forwarder = import "include/forwarder.jsonnet";
local qdrouterd = import "include/qdrouterd.jsonnet";
local addressController = import "include/address-controller.jsonnet";
local queueScheduler = import "include/queue-scheduler.jsonnet";
local subserv = import "include/subserv.jsonnet";
local common = import "include/common.jsonnet";
local flavor = import "include/flavor.jsonnet";
local messagingService = import "include/messaging-service.jsonnet";
local admin = import "include/admin.jsonnet";
local mqttGateway = import "include/mqtt-gateway.jsonnet";
local mqtt = import "include/mqtt.jsonnet";
local mqttService = import "include/mqtt-service.jsonnet";
{
  "messaging-service.json": messagingService.generate(false, false),
  "tls-messaging-service.json": messagingService.generate(true, false),
  "qdrouterd-imagestream.json": router.imagestream("enmasseproject/qdrouterd"),
  "qdrouterd-dc.json": qdrouterd.deployment(false),
  "artemis-imagestream.json": broker.imagestream("enmasseproject/artemis"),
  "forwarder-imagestream.json": forwarder.imagestream("enmasseproject/topic-forwarder"),
  "tls-qdrouterd-dc.json": qdrouterd.deployment(true),
  "configserv-dc.json": configserv.deployment,
  "configserv-imagestream.json": configserv.imagestream("enmasseproject/configserv"),
  "configuration-service.json": configserv.service,
  "address-controller-dc.json": addressController.deployment,
  "address-controller-imagestream.json": addressController.imagestream("enmasseproject/address-controller"),
  "address-controller-service.json": addressController.service,
  "queue-scheduler-dc.json": queueScheduler.deployment,
  "queue-scheduler-imagestream.json": queueScheduler.imagestream("enmasseproject/queue-scheduler"),
  "queue-scheduler-service.json": queueScheduler.service,
  "ragent-image-stream.json": ragent.imagestream("enmasseproject/ragent"),
  "ragent-dc.json": ragent.deployment,
  "ragent-service.json": ragent.service,
  "subserv-image-stream.json": subserv.imagestream("enmasseproject/subserv"),
  "subserv-dc.json": subserv.deployment,
  "subscription-service.json": subserv.service,
  "restapi-service.json": addressController.restapi,
  "flavor.json": flavor.generate(false),
  "tls-flavor.json": flavor.generate(true),
  "admin-dc.json": admin.deployment,
  "mqtt-gateway-image-stream.json": mqtt.imagestream("enmasseproject/mqtt-gateway"),
  "mqtt-gateway-dc.json": mqttGateway.deployment(false),
  "mqtt-service.json": mqttService.generate(false),
  "tls-mqtt-gateway-dc.json" : mqttGateway.deployment(true),
  "tls-mqtt-service.json" : mqttService.generate(true)
}
