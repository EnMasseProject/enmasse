local template = import "include/enmasse-template.jsonnet";
{
    "enmasse-template.json": template.generate(false, true, false),
    "tls-enmasse-template.json": template.generate(true, true, false),
    "enmasse-template-full.json": template.generate(false, false, false),
    "tls-enmasse-template-full.json": template.generate(true, false, false),
    "enmasse-template-with-kafka.json": template.generate(false, true, true),
    "tls-enmasse-template-with-kafka.json": template.generate(true, true, true)
}
