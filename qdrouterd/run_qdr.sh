#!/bin/sh

export MY_IP_ADDR=$(hostname -I)
envsubst < /etc/qpid-dispatch/qdrouterd.conf.template > /tmp/qdrouterd.conf
exec /sbin/qdrouterd --conf /tmp/qdrouterd.conf
