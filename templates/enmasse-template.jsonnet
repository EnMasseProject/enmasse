local template = import "include/enmasse-template.jsonnet";
{
    "enmasse-template.json": template.generate(false),
    "tls-enmasse-template.json": template.generate(true)
}
