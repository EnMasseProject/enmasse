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
local mqttLwt = import "include/mqtt-lwt.jsonnet";
local amqpKafkaBridge = import "include/amqp-kafka-bridge.jsonnet";
local amqpKafkaBridgeService = import "include/amqp-kafka-bridge-service.jsonnet";
{
  "messaging-service.json": messagingService.generate(false, false),
  "tls-messaging-service.json": messagingService.generate(true, false),
  "qdrouterd-dc.json": qdrouterd.deployment(false, "enmasseproject/qdrouterd"),
  "tls-qdrouterd-dc.json": qdrouterd.deployment(true, "enmasseproject/qdrouterd"),
  "configserv-dc.json": configserv.deployment("enmasseproject/configserv"),
  "configuration-service.json": configserv.service,
  "address-controller-dc.json": addressController.deployment("enmasseproject/address-controller"),
  "address-controller-service.json": addressController.service,
  "queue-scheduler-dc.json": queueScheduler.deployment("enmasseproject/queue-scheduler"),
  "queue-scheduler-service.json": queueScheduler.service,
  "ragent-dc.json": ragent.deployment("enmasseproject/ragent"),
  "ragent-service.json": ragent.service,
  "subserv-dc.json": subserv.deployment("enmasseproject/subserv"),
  "subscription-service.json": subserv.service,
  "restapi-service.json": addressController.restapi,
  "flavor.json": flavor.generate(false),
  "tls-flavor.json": flavor.generate(true),
  "admin-dc.json": admin.deployment("enmasseproject/address-controller", "enmasseproject/configserv", "enmasseproject/ragent", "enmasseproject/queue-scheduler"),
  "mqtt-gateway-dc.json": mqttGateway.deployment(false, "enmasseproject/mqtt-gateway"),
  "mqtt-service.json": mqttService.generate(false),
  "tls-mqtt-gateway-dc.json" : mqttGateway.deployment(true, "enmasseproject/mqtt-gateway"),
  "tls-mqtt-service.json" : mqttService.generate(true),
  "mqtt-lwt-dc.json" : mqttLwt.deployment("enmasseproject/mqtt-lwt"),
  "amqp-kafka-bridge-dc.json" : amqpKafkaBridge.deployment("enmasseproject/amqp-kafka-bridge"),
  "amqp-kafka-bridge-service.json" : amqpKafkaBridgeService.generate(false)
}
