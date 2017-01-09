local common = import "common.jsonnet";
{
  imagestream(image_name)::
    common.imagestream("mqtt-frontend", image_name),

  container(secure)::
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
      "image": "mqtt-frontend",
      "name": "mqtt-frontend",
      "ports": if secure
        then [secureMqttPort]
        else [mqttPort],
      "livenessProbe": {
        "tcpSocket": {
          "port": if secure
            then "secure-mqtt"
            else "mqtt"
        }
      },
      [if secure then "volumeMounts"]: [
        {
          "name": "ssl-certs",
          "mountPath": "/etc/mqtt-frontend/ssl",
          "readOnly": true
        }
      ]
    },

  secret_volume()::
    {
      "name": "ssl-certs",
      "secret": {
        "secretName": "mqtt-certs"
      }
    }
}
