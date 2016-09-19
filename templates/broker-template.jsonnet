local storage = import "include/storage-template.jsonnet";
local direct = import "include/direct-template.jsonnet";
{
  "queue-inmemory-template.json": storage.template(false, false, false),
  "topic-inmemory-template.json": storage.template(true, false, false),
  "queue-persisted-template.json": storage.template(false, true, false),
  "topic-persisted-template.json": storage.template(true, true, false),
  "tls-queue-inmemory-template.json": storage.template(false, false, true),
  "tls-topic-inmemory-template.json": storage.template(true, false, true),
  "tls-queue-persisted-template.json": storage.template(false, true, true),
  "tls-topic-persisted-template.json": storage.template(true, true, true),
  "anycast-template.json": direct.template(false),
  "broadcast-template.json": direct.template(true)
}
