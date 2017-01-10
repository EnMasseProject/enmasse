#!/bin/sh

if [ -d /etc/mqtt-frontend/ssl ]; then
  export ENMASSE_MQTT_SSL=true
  export ENMASSE_MQTT_KEYFILE='/etc/mqtt-frontend/ssl/server-key.pem'
  export ENMASSE_MQTT_CERTFILE='/etc/mqtt-frontend/ssl/server-cert.pem'
  export ENMASSE_MQTT_LISTENPORT=8883
fi

exec java -Dvertx.disableFileCaching=true -Dvertx.disableFileCPResolving=true -jar /mqtt-frontend-1.0-SNAPSHOT.jar
