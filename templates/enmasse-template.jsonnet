local template = import "include/enmasse-template.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
{
    "openshift/enmasse.json": template.generate(false, false),
    "openshift/enmasse-with-kafka.json": template.generate(false, true),
    "openshift/enmasse-with-sasldb.json": template.generate(true, false),
    "kubernetes/enmasse.json": kubernetes.common(false, false),
    "kubernetes/addons/external-lb.json": kubernetes.external_lb
}
