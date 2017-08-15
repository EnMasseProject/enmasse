#!/bin/sh
KEYCLOAK_DIR=/opt/jboss/keycloak
KEYCLOAK_CONFIG=${KEYCLOAK_DIR}/standalone/configuration/standalone.xml

java -jar /usr/share/java/saxon.jar -s:${KEYCLOAK_CONFIG} -xsl:${KEYCLOAK_DIR}/addSaslPlugin.xsl -o:${KEYCLOAK_CONFIG}
