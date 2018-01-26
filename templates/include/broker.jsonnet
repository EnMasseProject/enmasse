local common = import "common.jsonnet";
local authService = import "auth-service.jsonnet";
{
  container(volumeName, image_repo, addressEnv)::
    {
      "name": "broker",
      "image": image_repo,
      "ports": [
        common.container_port("amqp", 5673),
        common.container_port("jolokia", 8161),
        common.container_port("artemismetrics", 8080)
      ],
      "env": [
        addressEnv,
        common.env("CLUSTER_ID", "${CLUSTER_ID}"),
        common.env("CERT_DIR", "/etc/enmasse-certs")
      ] + authService.envVars,
      "volumeMounts": [
        common.volume_mount(volumeName, "/var/run/artemis"),
        common.volume_mount("broker-internal-cert", "/etc/enmasse-certs", true),
        common.volume_mount("authservice-ca", "/etc/authservice-ca", true),
        common.volume_mount("broker-prometheus-config", "/etc/prometheus-config", true)
      ],
      "resources": {
        "requests": {
          "memory": "${BROKER_MEMORY_LIMIT}"
        },
        "limits": {
          "memory": "${BROKER_MEMORY_LIMIT}"
        }
      },
      "livenessProbe": common.exec_probe(["sh", "-c", "$ARTEMIS_HOME/bin/probe.sh"], 120),
      "readinessProbe": common.exec_probe(["sh", "-c", "$ARTEMIS_HOME/bin/probe.sh"], 10),
      "lifecycle": {
        "preStop": {
          "exec": {
            "command": [
              "/shutdown-hook/shutdown-hook.sh"
           ]
          }
        }
      }
    },
}
