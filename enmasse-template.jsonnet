local template = import "include/enmasse-template.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
{
    "enmasse-template.json": template.generate(false, false),
    "enmasse-kubernetes.json": kubernetes.generate(false, false),
    "enmasse-template-with-kafka.json": template.generate(false, true),
    "sasldb-enmasse-template.json": template.generate(true, false)
}
