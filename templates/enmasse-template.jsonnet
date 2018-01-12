local openshift = import "include/enmasse-openshift.jsonnet";
local roles = import "include/roles.jsonnet";
local authService = import "include/auth-service.jsonnet";
local kubernetes = import "include/enmasse-kubernetes.jsonnet";
local prometheus = import "include/prometheus.jsonnet";
local grafana = import "include/grafana.jsonnet";
{
    "openshift/enmasse.json": openshift.template,
    "openshift/cluster-roles.json": openshift.cluster_roles,
    "openshift/addons/standard-authservice.json": authService.keycloak_openshift,
    "openshift/addons/none-authservice.json": authService.none_openshift,
    "openshift/addons/prometheus.json": prometheus.prometheus,
    "openshift/addons/grafana.json": grafana.grafana,
    "openshift/addons/cluster-reader.json": roles.cluster_reader,
    "kubernetes/enmasse.json": kubernetes.list,
    "kubernetes/addons/external-lb.json": kubernetes.external_lb,
    "kubernetes/addons/prometheus.json": prometheus.prometheus,
    "kubernetes/addons/grafana.json": grafana.grafana,
    "kubernetes/addons/cluster-reader.json": roles.cluster_reader,
    "kubernetes/cluster-roles.json": kubernetes.cluster_roles,
    "kubernetes/addons/standard-authservice.json": authService.keycloak_kubernetes,
    "kubernetes/addons/keycloak-controller.json": authService.keycloak_controller_kubernetes,
    "kubernetes/addons/none-authservice.json": authService.none_kubernetes,
}
