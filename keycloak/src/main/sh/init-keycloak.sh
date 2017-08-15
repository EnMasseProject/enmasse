#!/bin/sh
KEYCLOAK_DIR=/opt/jboss/keycloak
KEYCLOAK_CONFIG=${KEYCLOAK_DIR}/standalone/configuration/standalone.xml

java -jar /usr/share/java/saxon.jar -s:${KEYCLOAK_CONFIG} -xsl:${KEYCLOAK_DIR}/addSaslPlugin.xsl -o:${KEYCLOAK_CONFIG}
java -jar /usr/share/java/saxon.jar -s:${KEYCLOAK_CONFIG} -xsl:${KEYCLOAK_DIR}/removeFileLogging.xsl -o:${KEYCLOAK_CONFIG}

mkdir ${KEYCLOAK_DIR}/standalone/{data,log}
chown -R jboss:root ${KEYCLOAK_DIR}
find ${KEYCLOAK_DIR} -type d -exec chmod 770 {} \;
find ${KEYCLOAK_DIR}/standalone/configuration -type f -exec chmod 660 {} \;

