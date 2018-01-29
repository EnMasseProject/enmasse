#!/bin/sh
KEYCLOAK_CONFIG=${KEYCLOAK_DIR}/standalone/configuration/

cp ${KEYCLOAK_PLUGIN_DIR}/configuration/* ${KEYCLOAK_CONFIG}/
cp ${KEYCLOAK_PLUGIN_DIR}/providers/* ${KEYCLOAK_DIR}/providers/

KEYSTORE_PATH=${KEYCLOAK_DIR}/standalone/configuration/certificates.keystore
CERT_PATH=/opt/enmasse/cert

rm -f ${KEYSTORE_PATH}
openssl pkcs12 -export -passout pass:enmasse -in ${CERT_PATH}/tls.crt -inkey ${CERT_PATH}/tls.key -name "server" -out /tmp/certificates-keystore.p12
keytool -importkeystore -srcstorepass enmasse -deststorepass enmasse -destkeystore ${KEYSTORE_PATH} -srckeystore /tmp/certificates-keystore.p12 -srcstoretype PKCS12
