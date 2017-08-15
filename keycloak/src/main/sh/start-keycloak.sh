#!/bin/sh
KEYCLOAK_DIR=/opt/jboss/keycloak
KEYCLOAK_CONFIG=${KEYCLOAK_DIR}/standalone/configuration/standalone.xml
PASSWORD_SECRET=/var/run/keycloak/admin.key

if [ -f "${PASSWORD_SECRET}" ]; then
    ${KEYCLOAK_DIR}/bin/add-user-keycloak.sh -u admin -p `cat ${PASSWORD_SECRET}`
fi

exec /opt/jboss/keycloak/bin/standalone.sh $@
exit $?
