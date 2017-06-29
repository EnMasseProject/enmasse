local storage = import "include/storage-template.jsonnet";
{
  "queue-inmemory.json": storage.template(false, false),
  "topic-inmemory.json": storage.template(true, false),
  "queue-persisted.json": storage.template(false, true),
  "topic-persisted.json": storage.template(true, true)
}
