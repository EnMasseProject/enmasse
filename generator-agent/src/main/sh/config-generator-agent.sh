#!/bin/bash
SERVICE_ARGS=""

if [ "$KUBERNETES_SERVICE_HOST" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -o https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_SERVICE_PORT"
elif [ "$OPENSHIFT_URI" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -o $OPENSHIFT_URI"
fi

SERVICE_ARGS="$SERVICE_ARGS -c $CONFIGURATION_SERVICE_HOST:$CONFIGURATION_SERVICE_PORT"

echo "Args is $SERVICE_ARGS"

exec /usr/bin/java -Dvertx.cacheDirBase=/tmp/vert.x -jar /config-generator-agent.jar $SERVICE_ARGS
