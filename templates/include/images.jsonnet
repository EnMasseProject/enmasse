local version = std.extVar("VERSION");
local project = std.extVar("PROJECT");
local registry = std.extVar("REGISTRY");
{
  image(name)::
    registry + "/" + project + "/" + name + ":" + version,
    
  address_controller::
    self.image(std.extVar("ADDRESS_CONTROLLER_IMAGE")),

  standard_controller::
    self.image(std.extVar("STANDARD_CONTROLLER_IMAGE")),

  router::
    self.image(std.extVar("ROUTER_IMAGE")),

  artemis::
    self.image(std.extVar("ARTEMIS_IMAGE")),

  topic_forwarder::
    self.image(std.extVar("TOPIC_FORWARDER_IMAGE")),

  router_metrics::
    self.image(std.extVar("ROUTER_METRICS_IMAGE")),

  configserv::
    self.image(std.extVar("CONFIGSERV_IMAGE")),

  queue_scheduler::
    self.image(std.extVar("QUEUE_SCHEDULER_IMAGE")),

  agent::
    self.image(std.extVar("AGENT_IMAGE")),

  mqtt_gateway::
    self.image(std.extVar("MQTT_GATEWAY_IMAGE")),

  mqtt_lwt::
    self.image(std.extVar("MQTT_LWT_IMAGE")),

  none_authservice::
    self.image(std.extVar("NONE_AUTHSERVICE_IMAGE")),

  keycloak_plugin::
    self.image(std.extVar("KEYCLOAK_PLUGIN_IMAGE")),

  keycloak::
    std.extVar("KEYCLOAK_IMAGE"),

  keycloak_controller::
    self.image(std.extVar("KEYCLOAK_CONTROLLER_IMAGE")),

  prometheus::
    "prom/prometheus:v2.0.0",

  grafana::
    "enmasseproject/grafana:latest"
}
