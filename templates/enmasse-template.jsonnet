local openshift = import "include/enmasse-openshift.jsonnet";
local authService = import "include/auth-service.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
{
    "openshift/enmasse.json": openshift.template,
    "openshift/cluster-roles.json": openshift.cluster_roles,
    "openshift/addons/standard-authservice.json": authService.keycloak_openshift,
    "openshift/addons/none-authservice.json": authService.none_openshift,
    "kubernetes/enmasse.json": kubernetes.list,
    "kubernetes/addons/external-lb.json": kubernetes.external_lb,
    "kubernetes/cluster-roles.json": kubernetes.cluster_roles,
    "kubernetes/addons/standard-authservice.json": authService.keycloak_kubernetes,
    "kubernetes/addons/none-authservice.json": authService.none_kubernetes,
}
