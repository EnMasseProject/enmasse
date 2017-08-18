#!/bin/sh
KEYCLOAK_DIR=/opt/jboss/keycloak

if [ -n ${ADMIN_USER} ] && [ -n ${ADMIN_PASSWORD} ]; then
    ${KEYCLOAK_DIR}/bin/add-user-keycloak.sh -u ${ADMIN_USER} -p ${ADMIN_PASSWORD}
fi

export JAVA_OPTS="-Dvertx.cacheDirBase=/tmp -Djboss.bind.address=0.0.0.0"

exec /opt/jboss/keycloak/bin/standalone.sh $@
exit $?
