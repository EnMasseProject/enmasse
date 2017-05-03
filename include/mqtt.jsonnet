local version = std.extVar("VERSION");
local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("mqtt-gateway", image_name),

  container(secure, image_repo)::
    local mqttPort = {
      "name": "mqtt",
      "containerPort": 1883,
      "protocol": "TCP"
    };
    local secureMqttPort = {
      "name": "secure-mqtt",
      "containerPort": 8883,
      "protocol": "TCP"
    };
    {
      "image": image_repo + ":" + version,
      "name": if secure
        then "mqtt-gateway-tls"
        else "mqtt-gateway",
      "ports": if secure
        then [secureMqttPort]
        else [mqttPort],
      "livenessProbe": {
        "initialDelaySeconds": 60,
        "tcpSocket": {
          "port": if secure
            then "secure-mqtt"
            else "mqtt"
        }
      },
      [if secure then "volumeMounts"]: [
        {
          "name": "ssl-certs",
          "mountPath": "/etc/mqtt-gateway/ssl",
          "readOnly": true
        }
      ]
    },

  secret_volume(mqtt_secret)::
    {
      "name": "ssl-certs",
      "secret": {
        "secretName": mqtt_secret
      }
    }
}
