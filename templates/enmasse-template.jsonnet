local template = import "include/enmasse-template.jsonnet";
local authService = import "include/auth-service.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
{
    "openshift/enmasse.json": template.generate(false, false),
    "openshift/addons/keycloak.json": authService.openshift,
    "openshift/enmasse-with-kafka.json": template.generate(false, true),
    "openshift/enmasse-with-sasldb.json": template.generate(true, false),
    "kubernetes/enmasse.json": kubernetes.common(false, false),
    "kubernetes/addons/external-lb.json": kubernetes.external_lb,
    "kubernetes/addons/keycloak.json": authService.kubernetes
}
