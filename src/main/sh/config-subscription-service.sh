#!/bin/bash
SERVICE_ARGS=""
if [ "$OPENSHIFT_USER" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -u $OPENSHIFT_USER"
fi

if [ "$OPENSHIFT_TOKEN" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -t $OPENSHIFT_TOKEN"
fi

if [ "$OPENSHIFT_NAMESPACE" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -n $OPENSHIFT_NAMESPACE"
fi

if [ "$KUBERNETES_SERVICE_HOST" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -s https://$KUBERNETES_SERVICE_HOST:$KUBERNETES_SERVICE_PORT"
elif [ "$OPENSHIFT_URI" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -s $OPENSHIFT_URI"
fi

#if [ "$AMQP_LISTEN_ADDRESS"=="" ]; then
#    AMQP_LISTEN_ADDRESS=$(hostname -I)
#fi

if [ "$AMQP_LISTEN_ADDRESS" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -l $AMQP_LISTEN_ADDR"
fi

if [ "$AMQP_PORT" != "" ]; then
    SERVICE_ARGS="$SERVICE_ARGS -p $AMQP_PORT"
fi

echo "Args is $SERVICE_ARGS"

exec /usr/bin/java -Dvertx.cacheDirBase=/tmp/vert.x -jar /config-subscription-service.jar $SERVICE_ARGS
