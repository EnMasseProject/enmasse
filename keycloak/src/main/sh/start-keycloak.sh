#!/bin/bash
KEYSTORE_PATH=${JBOSS_HOME}/standalone/configuration/certificates.keystore
CERT_PATH=/opt/enmasse/cert

openssl pkcs12 -export -passout pass:enmasse -in ${CERT_PATH}/tls.crt -inkey ${CERT_PATH}/tls.key -name "server" -out /tmp/certificates-keystore.p12
keytool -importkeystore -srcstorepass enmasse -deststorepass enmasse -destkeystore ${KEYSTORE_PATH} -srckeystore /tmp/certificates-keystore.p12 -srcstoretype PKCS12

/opt/jboss/docker-entrypoint.sh $@
