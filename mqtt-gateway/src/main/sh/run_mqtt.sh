#!/bin/sh

if [ -d /etc/mqtt-gateway/ssl ]; then
  export ENMASSE_MQTT_SSL=true
  export ENMASSE_MQTT_KEYFILE='/etc/mqtt-gateway/ssl/server-key.pem'
  export ENMASSE_MQTT_CERTFILE='/etc/mqtt-gateway/ssl/server-cert.pem'
  export ENMASSE_MQTT_LISTENPORT=8883
fi

exec java -Dvertx.cacheDirBase=/tmp -jar /mqtt-gateway.jar
