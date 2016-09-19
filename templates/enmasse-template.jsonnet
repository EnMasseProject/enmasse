local template = import "include/enmasse-template.jsonnet";
{
    "enmasse-template.json": template.generate(false, true),
    "tls-enmasse-template.json": template.generate(true, true),
    "enmasse-no-controller-template.json": template.generate(false, false)
}
