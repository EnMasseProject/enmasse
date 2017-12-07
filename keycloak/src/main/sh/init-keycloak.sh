#!/bin/sh
KEYCLOAK_DIR=$JBOSS_HOME
KEYCLOAK_CONFIG=${KEYCLOAK_DIR}/standalone/configuration/standalone.xml

xsltproc ${KEYCLOAK_DIR}/addSaslPlugin.xsl ${KEYCLOAK_CONFIG} > ${KEYCLOAK_CONFIG}.new; mv ${KEYCLOAK_CONFIG}.new ${KEYCLOAK_CONFIG}
xsltproc ${KEYCLOAK_DIR}/removeFileLogging.xsl ${KEYCLOAK_CONFIG} > ${KEYCLOAK_CONFIG}.new; mv ${KEYCLOAK_CONFIG}.new ${KEYCLOAK_CONFIG}
xsltproc ${KEYCLOAK_DIR}/addKeyStore.xsl ${KEYCLOAK_CONFIG} > ${KEYCLOAK_CONFIG}.new; mv ${KEYCLOAK_CONFIG}.new ${KEYCLOAK_CONFIG}

chown -R jboss:root ${KEYCLOAK_DIR}
find ${KEYCLOAK_DIR} -type d -exec chmod 770 {} \;
find ${KEYCLOAK_DIR}/standalone/configuration -type f -exec chmod 660 {} \;

