#!/bin/sh

export MY_IP_ADDR=$(hostname -I)
if [ -z "$RAGENT_SERVICE_HOST" ]; then
    echo "ERROR: router agent service not configured";
    export RAGENT_SERVICE_HOST=localhost;
    export RAGENT_SERVICE_PORT=55672;
fi
envsubst < /etc/qpid-dispatch/qdrouterd.conf.template > /tmp/qdrouterd.conf
if [ -n "$QUEUE_NAME" ]; then
    export ADDRESS_NAME=$QUEUE_NAME;
    envsubst < /etc/qpid-dispatch/colocated.snippet >> /tmp/qdrouterd.conf
elif [ -n "$TOPIC_NAME" ]; then
    export ADDRESS_NAME=$TOPIC_NAME;
    envsubst < /etc/qpid-dispatch/colocated.snippet >> /tmp/qdrouterd.conf
fi
exec /sbin/qdrouterd --conf /tmp/qdrouterd.conf
