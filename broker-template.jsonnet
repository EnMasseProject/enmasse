local storage = import "include/storage-template.jsonnet";
{
  "queue-inmemory-template.json": storage.template(false, false),
  "topic-inmemory-template.json": storage.template(true, false),
  "queue-persisted-template.json": storage.template(false, true),
  "topic-persisted-template.json": storage.template(true, true)
}
