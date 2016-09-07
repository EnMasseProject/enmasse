local storage = import "include/storage.jsonnet";
{
  "queue-inmemory.json": storage.template(false, false, false),
  "secure-queue-inmemory.json": storage.template(false, false, true),
  "topic-inmemory.json": storage.template(true, false, false),
  "secure-topic-inmemory.json": storage.template(true, false, true)
}
