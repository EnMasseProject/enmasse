local template = import "include/enmasse-template.jsonnet";
{
    "enmasse-template.json": template.generate(false),
    "secure-enmasse-template.json": template.generate(true)
}
