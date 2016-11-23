local configserv = import "include/configserv.jsonnet";
local ragent = import "include/ragent.jsonnet";
local router = import "include/router.jsonnet";
local broker = import "include/broker.jsonnet";
local forwarder = import "include/forwarder.jsonnet";
local qdrouterd = import "include/qdrouterd.jsonnet";
local storageController = import "include/storage-controller.jsonnet";
local subserv = import "include/subserv.jsonnet";
local restapi = import "include/restapi.jsonnet";
local messagingService = import "include/messaging-service.jsonnet";
{
  "messaging-service.json": messagingService.generate(false),
  "tls-messaging-service.json": messagingService.generate(true),
  "qdrouterd-imagestream.json": router.imagestream("enmasseproject/qdrouterd"),
  "qdrouterd-dc.json": qdrouterd.deployment(false),
  "artemis-imagestream.json": broker.imagestream("enmasseproject/artemis"),
  "forwarder-imagestream.json": forwarder.imagestream("enmasseproject/topic-forwarder"),
  "tls-qdrouterd-dc.json": qdrouterd.deployment(true),
  "configserv-dc.json": configserv.deployment,
  "configserv-imagestream.json": configserv.imagestream("enmasseproject/configserv"),
  "storage-controller-dc.json": storageController.deployment,
  "storage-controller-imagestream.json": storageController.imagestream("enmasseproject/storage-controller"),
  "ragent-image-stream.json": ragent.imagestream("enmasseproject/ragent"),
  "ragent-dc.json": ragent.deployment,
  "subserv-image-stream.json": subserv.imagestream("enmasseproject/subserv"),
  "subserv-dc.json": subserv.deployment,
  "restapi-image-stream.json": restapi.imagestream("enmasseproject/enmasse-rest"),
  "restapi-dc.json": restapi.deployment
}
