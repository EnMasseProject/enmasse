local template = import "include/enmasse-template.jsonnet";
local full_template = import "include/enmasse-template-full.jsonnet";
local template_with_kafka = import "include/enmasse-template-with-kafka.jsonnet";
{
    "enmasse-template.json": template.generate(false),
    "tls-enmasse-template.json": template.generate(true),
    "enmasse-base-template.json": full_template.generate(false, false),
    "tls-enmasse-base-template.json": full_template.generate(true, false),
    "enmasse-template-full.json": full_template.generate(false, true),
    "tls-enmasse-template-full.json": full_template.generate(true, true),
    "enmasse-template-with-kafka.json": template_with_kafka.generate(false),
    "tls-enmasse-template-with-kafka.json": template_with_kafka.generate(true)
}
