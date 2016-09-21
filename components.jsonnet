local version = std.extVar("VERSION");
local configmapBridge = import "include/configmap-bridge.jsonnet";
local ragent = import "include/ragent.jsonnet";
local router = import "include/router.jsonnet";
local broker = import "include/broker.jsonnet";
local forwarder = import "include/forwarder.jsonnet";
local qdrouterd = import "include/qdrouterd.jsonnet";
local storageController = import "include/storage-controller.jsonnet";
local subserv = import "include/subserv.jsonnet";
local messagingService = import "include/messaging-service.jsonnet";
{
  "messaging-service.json": messagingService.generate(false),
  "tls-messaging-service.json": messagingService.generate(true),
  "qdrouterd-imagestream.json": router.imagestream("gordons/qdrouterd:" + version),
  "qdrouterd-dc.json": qdrouterd.deployment(false),
  "artemis-imagestream.json": broker.imagestream("enmasseproject/artemis:" + version),
  "forwarder-imagestream.json": forwarder.imagestream("enmasseproject/topic-forwarder:" + version),
  "tls-qdrouterd-dc.json": qdrouterd.deployment(true),
  "configmap-bridge-dc.json": configmapBridge.deployment,
  "configmap-bridge-imagestream.json": configmapBridge.imagestream("enmasseproject/configmap-bridge:" + version),
  "storage-controller-dc.json": storageController.deployment,
  "storage-controller-imagestream.json": storageController.imagestream("enmasseproject/storage-controller:" + version),
  "ragent-image-stream.json": ragent.imagestream("enmasseproject/ragent:" + version),
  "ragent-dc.json": ragent.deployment,
  "subserv-image-stream.json": subserv.imagestream("enmasseproject/subserv:" + version),
  "subserv-dc.json": subserv.deployment
}
