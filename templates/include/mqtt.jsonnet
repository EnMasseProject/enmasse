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
      "image": image_repo,
      [if secure then "env"]: [
        common.env("ENMASSE_MQTT_SSL", "true"),
        common.env("ENMASSE_MQTT_KEYFILE", "/etc/mqtt-gateway/ssl/tls.key"),
        common.env("ENMASSE_MQTT_CERTFILE", "/etc/mqtt-gateway/ssl/tls.crt"),
        common.env("ENMASSE_MQTT_LISTENPORT", "8883")
      ],
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
