local template = import "include/enmasse-template.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
{
    "enmasse-template.json": template.generate(false, false, true, false),
    "enmasse-kubernetes.json": kubernetes.generate(false, false, true, false),
    "tls-enmasse-template.json": template.generate(true, false, true, false),
    "enmasse-template-full.json": template.generate(false, false, false, false),
    "tls-enmasse-template-full.json": template.generate(true, false, false, false),
    "enmasse-template-with-kafka.json": template.generate(false, false, true, true),
    "tls-enmasse-template-with-kafka.json": template.generate(true, false, true, true),
    "sasldb-enmasse-template.json": template.generate(false, true, true, false),
    "sasldb-tls-enmasse-template.json": template.generate(true, true, true, false),
}
