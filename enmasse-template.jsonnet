local template = import "include/enmasse-template.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
{
    "enmasse-openshift.json": template.generate(false, false),
    "enmasse-openshift-with-kafka.json": template.generate(false, true),
    "enmasse-openshift-with-sasldb.json": template.generate(true, false),
    "enmasse-kubernetes.json": kubernetes.common(false, false),
    // "enmasse-kubernetes-external-lb.json": kubernetes.external_lb(false, false)
}
