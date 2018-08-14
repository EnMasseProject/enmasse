#!/bin/sh

CONFIG_FILE=/tmp/qdrouterd.conf
WORKING_DIR=/etc/qpid-dispatch/

${WORKING_DIR}/configure.sh ${WORKING_DIR} $CONFIG_FILE

if [ -f $CONFIG_FILE ]; then
    ARGS="-c $CONFIG_FILE"
fi

exec /sbin/qdrouterd $ARGS
