local storage = import "include/storage-template.jsonnet";
local router_image = "gordons/qdrouterd:latest";
local default_broker_image = "enmasseproject/artemis:latest";
{
  "queue-inmemory.json": storage.template(false, false, false, router_image, default_broker_image),
  "topic-inmemory.json": storage.template(true, false, false, router_image, default_broker_image),
  "queue-persisted.json": storage.template(false, true, false, router_image, default_broker_image),
  "topic-persisted.json": storage.template(true, true, false, router_image, default_broker_image),
  "tls-queue-inmemory.json": storage.template(false, false, true, router_image, default_broker_image),
  "tls-topic-inmemory.json": storage.template(true, false, true, router_image, default_broker_image),
  "tls-queue-persisted.json": storage.template(false, true, true, router_image, default_broker_image),
  "tls-topic-persisted.json": storage.template(true, true, true, router_image, default_broker_image)
}
