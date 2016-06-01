#!/bin/bash
SERVICE_ARGS=""

if [ "$KUBERNETES_SERVICE_HOST" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -s https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_SERVICE_PORT"
elif [ "$OPENSHIFT_URI" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -s $OPENSHIFT_URI"
fi

if [ "$AMQP_LISTEN_ADDRESS" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -l $AMQP_LISTEN_ADDR"
fi

if [ "$AMQP_PORT" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -p $AMQP_PORT"
fi

echo "Args is $SERVICE_ARGS"

JAVA_OPTS="-Dvertx.cacheDirBase=/tmp/vert.x" exec /config-subscription-service-0.1/bin/config-subscription-service $SERVICE_ARGS
