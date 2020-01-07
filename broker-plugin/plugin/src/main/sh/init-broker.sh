#!/bin/sh

#ENV_VARS
BROKER_DIR=$ARTEMIS_HOME
BROKER_PLUGIN_DIR=/opt/broker-plugin
BROKER_CUSTOM=${BROKER_DIR}/custom
CONFIG_TEMPLATES=${BROKER_PLUGIN_DIR}/conf
BROKER_CONF_DIR=${BROKER_CUSTOM}/conf

export BROKER_IP=`hostname -f`

mkdir -p $BROKER_CONF_DIR
cp -r ${BROKER_PLUGIN_DIR}/lib $BROKER_CUSTOM
cp -r ${BROKER_PLUGIN_DIR}/bin $BROKER_CUSTOM

function configure_brokered() {
    DISABLE_AUTHORIZATION=$(echo ${DISABLE_AUTHORIZATION-false} | tr '[:upper:]' '[:lower:]')
    if [ "${DISABLE_AUTHORIZATION}" == "true" ]
    then
        export KEYCLOAK_GROUP_PERMISSIONS=false
        echo "export KEYCLOAK_GROUP_PERMISSIONS=false" >> $BROKER_CUSTOM/bin/env.sh
    else
        export KEYCLOAK_GROUP_PERMISSIONS=true
        echo "export KEYCLOAK_GROUP_PERMISSIONS=true" >> $BROKER_CUSTOM/bin/env.sh
    fi
    cp $CONFIG_TEMPLATES/brokered/broker.xml /tmp/broker.xml
    cp $CONFIG_TEMPLATES/brokered/login.config /tmp/login.config
    export HAWTIO_ROLE=manage
}

function configure_standard() {
    if [ -n "$TOPIC_NAME" ]; then
        cp $CONFIG_TEMPLATES/standard/sharded-topic/broker.xml /tmp/broker.xml
    elif [ -n $QUEUE_NAME ] && [ "$QUEUE_NAME" != "" ]; then
        cp $CONFIG_TEMPLATES/standard/sharded-queue/broker.xml /tmp/broker.xml
    else
        cp $CONFIG_TEMPLATES/standard/colocated/broker.xml /tmp/broker.xml
    fi

    cp $CONFIG_TEMPLATES/standard/login.config /tmp/login.config    
    export HAWTIO_ROLE=manage
}

function pre_configuration() {
    local instanceDir="${HOME}/${AMQ_NAME}"
    
    echo "export AMQ_USER=admin" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AMQ_PASSWORD=admin" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AMQ_REQUIRE_LOGIN=false" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AMQ_TRANSPORTS=amqp" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AMQ_NAME=$AMQ_NAME" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AMQ_ROLE=admin" >> $BROKER_CUSTOM/bin/env.sh
    echo "export ARTEMIS_HOME=$ARTEMIS_HOME" >> $BROKER_CUSTOM/bin/env.sh
    echo "export CONTAINER_ID=$HOSTNAME" >> $BROKER_CUSTOM/bin/env.sh
    echo "export KEYSTORE_PATH=$instanceDir/etc/enmasse-keystore.jks" >> $BROKER_CUSTOM/bin/env.sh
    echo "export TRUSTSTORE_PATH=$instanceDir/etc/enmasse-truststore.jks" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AUTH_TRUSTSTORE_PATH=$instanceDir/etc/enmasse-authtruststore.jks" >> $BROKER_CUSTOM/bin/env.sh
    echo "export EXTERNAL_KEYSTORE_PATH=$instanceDir/etc/external-keystore.jks" >> $BROKER_CUSTOM/bin/env.sh
    TRUSTSTORE_PASS=enmasse
    KEYSTORE_PASS=enmasse
    source $BROKER_CUSTOM/bin/env.sh

    # Make sure that we use /dev/urandom
    JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"
    echo "export JAVA_OPTS=\"${JAVA_OPTS} -Djavax.net.ssl.keyStore=${KEYSTORE_PATH} -Djavax.net.ssl.keyStorePassword=${KEYSTORE_PASS} -Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH} -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASS}\"" >> $BROKER_CUSTOM/bin/env.sh

    chmod a+x $BROKER_CUSTOM/bin/env.sh
    source $BROKER_CUSTOM/bin/env.sh

    export CONTAINER_ID=$HOSTNAME

    TRUSTSTORE_PASS=enmasse
    KEYSTORE_PASS=enmasse

    if [ -n "$ADMIN_SERVICE_HOST" ]; then
        export QUEUE_SCHEDULER_SERVICE_HOST=$ADMIN_SERVICE_HOST
        export QUEUE_SCHEDULER_SERVICE_PORT=$ADMIN_SERVICE_PORT_QUEUE_SCHEDULER
    fi

    if [ "$ADDRESS_SPACE_TYPE" == "brokered" ]; then
        configure_brokered
    else
        configure_standard
    fi

    envsubst < /tmp/broker.xml > $BROKER_CONF_DIR/broker.xml
    envsubst < /tmp/login.config > $BROKER_CONF_DIR/login.config

    cp $CONFIG_TEMPLATES/bootstrap.xml $BROKER_CONF_DIR/bootstrap.xml
    cp $CONFIG_TEMPLATES/jolokia-access.xml $BROKER_CONF_DIR/jolokia-access.xml
    cp /opt/broker-plugin/lib/broker-cli.jar $BROKER_CUSTOM/bin/broker-cli.jar

    export ARTEMIS_INSTANCE=${instanceDir}
    export ARTEMIS_INSTANCE_URI=file:${instanceDir}/
    export ARTEMIS_DATA_DIR=${instanceDir}/data
    export ARTEMIS_INSTANCE_ETC_URI=file:${instanceDir}/etc/
    
    envsubst < $CONFIG_TEMPLATES/artemis.profile > $BROKER_CONF_DIR/artemis.profile

    cp $CONFIG_TEMPLATES/logging.properties $BROKER_CONF_DIR/logging.properties
}

function configure_ssl() {
    TRUSTSTORE_PASS=enmasse
    KEYSTORE_PASS=enmasse

    export CUSTOM_KEYSTORE_PATH=$BROKER_CUSTOM/certs/enmasse-keystore.jks
    export CUSTOM_TRUSTSTORE_PATH=$BROKER_CUSTOM/certs/enmasse-truststore.jks
    export CUSTOM_AUTH_TRUSTSTORE_PATH=$BROKER_CUSTOM/certs/enmasse-authtruststore.jks
    export CUSTOM_EXTERNAL_KEYSTORE_PATH=$BROKER_CUSTOM/certs/external-keystore.jks
    mkdir -p $BROKER_CUSTOM/certs

    # Recreate certs in case they have been updated
    rm -rf ${CUSTOM_TRUSTSTORE_PATH} ${CUSTOM_KEYSTORE_PATH} ${CUSTOM_AUTH_TRUSTSTORE_PATH} ${CUSTOM_EXTERNAL_KEYSTORE_PATH}

    # Convert certs
    openssl pkcs12 -export -passout pass:${KEYSTORE_PASS} -in /etc/enmasse-certs/tls.crt -inkey /etc/enmasse-certs/tls.key -chain -CAfile /etc/enmasse-certs/ca.crt -name "io.enmasse" -out /tmp/enmasse-keystore.p12

    keytool -importkeystore -srcstorepass ${KEYSTORE_PASS} -deststorepass ${KEYSTORE_PASS} -destkeystore $CUSTOM_KEYSTORE_PATH -srckeystore /tmp/enmasse-keystore.p12 -srcstoretype PKCS12
    keytool -import -noprompt -file /etc/enmasse-certs/ca.crt -alias firstCA -deststorepass ${TRUSTSTORE_PASS} -keystore $CUSTOM_TRUSTSTORE_PATH

    cp /etc/pki/java/cacerts ${CUSTOM_AUTH_TRUSTSTORE_PATH}
    chmod 644 ${CUSTOM_AUTH_TRUSTSTORE_PATH}
    keytool -storepasswd -storepass changeit -new ${TRUSTSTORE_PASS} -keystore $CUSTOM_AUTH_TRUSTSTORE_PATH
    keytool -import -noprompt -file /etc/authservice-ca/tls.crt -alias firstCA -deststorepass ${TRUSTSTORE_PASS} -keystore $CUSTOM_AUTH_TRUSTSTORE_PATH

    if [ -d /etc/external-certs ]
    then
        openssl pkcs12 -export -passout pass:${KEYSTORE_PASS} -in /etc/external-certs/tls.crt -inkey /etc/external-certs/tls.key -name "io.enmasse" -out /tmp/external-keystore.p12
        keytool -importkeystore -srcstorepass ${KEYSTORE_PASS} -deststorepass ${KEYSTORE_PASS} -destkeystore $CUSTOM_EXTERNAL_KEYSTORE_PATH -srckeystore /tmp/external-keystore.p12 -srcstoretype PKCS12
    fi
}

pre_configuration
configure_ssl
echo "broker-plugin is complete"
