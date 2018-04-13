#!/bin/sh
KEYCLOAK_CONFIG=${KEYCLOAK_DIR}/standalone/configuration/

cp ${KEYCLOAK_PLUGIN_DIR}/configuration/* ${KEYCLOAK_CONFIG}/
cp ${KEYCLOAK_PLUGIN_DIR}/providers/* ${KEYCLOAK_DIR}/providers/

KEYSTORE_PATH=${KEYCLOAK_DIR}/standalone/configuration/certificates.keystore
CERT_PATH=/opt/enmasse/cert
OPENSHIFT_CA=/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
TRUSTSTORE_PATH=${KEYCLOAK_DIR}/standalone/configuration/truststore.jks

rm -f ${KEYSTORE_PATH}
openssl pkcs12 -export -passout pass:enmasse -in ${CERT_PATH}/tls.crt -inkey ${CERT_PATH}/tls.key -name "server" -out /tmp/certificates-keystore.p12
keytool -importkeystore -srcstorepass enmasse -deststorepass enmasse -destkeystore ${KEYSTORE_PATH} -srckeystore /tmp/certificates-keystore.p12 -srcstoretype PKCS12
echo "Keystore ${KEYSTORE_PATH} created"

rm -rf ${TRUSTSTORE_PATH}
keytool -import -noprompt -file ${OPENSHIFT_CA} -alias firstCA -deststorepass enmasse -keystore $TRUSTSTORE_PATH
echo "Truststore ${TRUSTSTORE_PATH} created"
