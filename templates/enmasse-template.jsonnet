local roles = import "include/roles.jsonnet";
local authService = import "include/auth-service.jsonnet";
local prometheus = import "include/prometheus.jsonnet";
local addressController = import "include/address-controller.jsonnet";
local serviceBroker = import "include/service-broker.jsonnet";
local grafana = import "include/grafana.jsonnet";
{
    "resources/grafana/grafana.json": grafana.grafana,
    "resources/prometheus/prometheus.json": prometheus.prometheus,
    "resources/cluster-roles/enmasse-address-admin.json": roles.address_admin_role,
    "resources/cluster-roles/enmasse-admin.json": roles.enmasse_admin_role,
    "resources/address-controller/deployment.json": addressController.deployment,
    "resources/address-controller/service.json": addressController.service,
    "resources/address-controller/route.json": addressController.route,
    "resources/address-controller/external-service.json": addressController.external_service,
    "resources/service-broker/deployment.json": serviceBroker.deployment,
    "resources/service-broker/service.json": serviceBroker.service,
    "resources/none-authservice/service.json": authService.none_authservice,
    "resources/none-authservice/deployment.json": authService.none_deployment,
    "resources/standard-authservice/service.json": authService.standard_authservice,
    "resources/standard-authservice/external-service.json": authService.standard_authservice_external,
    "resources/standard-authservice/pvc.json": authService.keycloak_pvc,
    "resources/standard-authservice/keycloak-deployment.json": authService.keycloak_deployment, 
    "resources/standard-authservice/controller-deployment.json": authService.keycloak_controller_deployment, 
    "resources/standard-authservice/route.json": authService.keycloak_route,
}
