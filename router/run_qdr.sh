#!/bin/sh

CONFIG_FILE=/tmp/qdrouterd.conf
WORKING_DIR=/etc/qpid-dispatch/

${WORKING_DIR}/configure.sh ${WORKING_DIR} $CONFIG_FILE

DEFAULT_AUTHENTICATION_SERVICE_CA_PATH="/etc/qpid-dispatch/authservice-ca/tls.crt"
AUTHENTICATION_SERVICE_CERT_DB=""
if [ -s /etc/qpid-dispatch/authservice-ca/tls.crt ]
then
    AUTHENTICATION_SERVICE_CERT_DB="certDb: ${DEFAULT_AUTHENTICATION_SERVICE_CA_PATH}"
fi

cat $CONFIG_FILE | envsubst '${AUTHENTICATION_SERVICE_CERT_DB}' > /tmp/cfg.tmp
mv /tmp/cfg.tmp $CONFIG_FILE

if [ -f $CONFIG_FILE ]; then
    ARGS="-c $CONFIG_FILE"
fi

exec /sbin/qdrouterd $ARGS
