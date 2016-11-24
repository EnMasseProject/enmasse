local template = import "include/enmasse-template.jsonnet";
local compact = import "include/enmasse-compact-template.jsonnet";
{
    "enmasse-template.json": template.generate(false, true),
    "tls-enmasse-template.json": template.generate(true, true),
    "enmasse-base-template.json": template.generate(false, false),
    "tls-enmasse-base-template.json": template.generate(true, false),
    "enmasse-compact-template.json": compact.generate(false),
    "tls-enmasse-compact-template.json": compact.generate(true)
}
