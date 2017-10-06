local common = import "common.jsonnet";
{
  container(volumeName, image_repo, addressEnv)::
    {
      "name": "broker",
      "image": image_repo,
      "ports": [
        common.container_port("amqp", 5673),
        common.container_port("jolokia", 8161)
      ],
      "env": [
        addressEnv,
        common.env("CLUSTER_ID", "${CLUSTER_ID}"),
        common.env("CERT_DIR", "/etc/enmasse-certs")
      ],
      "volumeMounts": [
        common.volume_mount(volumeName, "/var/run/artemis"),
        common.volume_mount("broker-internal-cert", "/etc/enmasse-certs", true)
      ],
      "livenessProbe": common.tcp_probe("amqp", 120),
      "readinessProbe": common.tcp_probe("amqp", 0),
      "lifecycle": {
        "preStop": {
          "exec": {
            "command": [
              "/opt/artemis-shutdown-hook/bin/artemis-shutdown-hook"
           ]
          }
        }
      }
    },
}
