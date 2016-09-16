local storage = import "include/storage-template.jsonnet";
{
  "queue-inmemory.json": storage.template(false, false, false),
  "topic-inmemory.json": storage.template(true, false, false),
  "queue-persisted.json": storage.template(false, true, false),
  "topic-persisted.json": storage.template(true, true, false),
  "tls-queue-inmemory.json": storage.template(false, false, true),
  "tls-topic-inmemory.json": storage.template(true, false, true),
  "tls-queue-persisted.json": storage.template(false, true, true),
  "tls-topic-persisted.json": storage.template(true, true, true)
}
