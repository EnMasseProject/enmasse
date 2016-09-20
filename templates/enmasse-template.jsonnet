local template = import "include/enmasse-template.jsonnet";
local version = "latest";
{
    "enmasse-template.json": template.generate(false, true, version),
    "tls-enmasse-template.json": template.generate(true, true, version),
    "enmasse-base-template.json": template.generate(false, false, version),
    "tls-enmasse-base-template.json": template.generate(true, false, version),
}
