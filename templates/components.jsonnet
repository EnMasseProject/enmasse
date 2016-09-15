local configmapBridge = import "include/configmap-bridge.jsonnet";
local ragent = import "include/ragent.jsonnet";
local qdrouterd = import "include/qdrouterd.jsonnet";
local storageController = import "include/storage-controller.jsonnet";
local messagingService = import "include/messaging-service.jsonnet";
{
  "messaging-service.json": messagingService.generate(false),
  "tls-messaging-service.json": messagingService.generate(true),
  "qdrouterd-rc.json": qdrouterd.generate(false),
  "tls-qdrouterd-rc.json": qdrouterd.generate(true),
  "configmap-bridge-dc.json": configmapBridge.deployment,
  "configmap-bridge-imagestream.json": configmapBridge.imagestream("enmasseproject/configmap-bridge:latest"),
  "storage-controller-dc.json": storageController.deployment,
  "storage-controller-imagestream.json": storageController.imagestream("enmasseproject/storage-controller:latest"),
  "ragent-rc.json": ragent.generate("enmasseproject/ragent:latest")
}
