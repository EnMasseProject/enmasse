local storage = import "include/storage-template.jsonnet";
{
  "queue-inmemory.json": storage.template(false, false, false),
  "tls-queue-inmemory.json": storage.template(false, false, true),
  "topic-inmemory.json": storage.template(true, false, false),
  "tls-topic-inmemory.json": storage.template(true, false, true)
}
