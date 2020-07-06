#!/bin/sh

set -e

#ENV_VARS
BROKER_DIR=$ARTEMIS_HOME
BROKER_PLUGIN_DIR=/opt/broker-plugin
BROKER_CUSTOM=${BROKER_DIR}/custom
CONFIG_TEMPLATES=${BROKER_PLUGIN_DIR}/conf
BROKER_CONF_DIR=${BROKER_CUSTOM}/conf
MESSAGE_EXPIRY_SCAN_PERIOD=${MESSAGE_EXPIRY_SCAN_PERIOD:-30000}
export MESSAGE_EXPIRY_SCAN_PERIOD

export BROKER_IP=`hostname -f`

mkdir -p $BROKER_CONF_DIR
cp -r ${BROKER_PLUGIN_DIR}/lib $BROKER_CUSTOM
cp -r ${BROKER_PLUGIN_DIR}/bin $BROKER_CUSTOM

function configure_shared() {
    cp $CONFIG_TEMPLATES/shared/broker.xml /tmp/broker.xml
    cp $CONFIG_TEMPLATES/shared/bootstrap.xml $BROKER_CONF_DIR/bootstrap.xml
    cp $CONFIG_TEMPLATES/shared/login.config /tmp/login.config
    cp $CONFIG_TEMPLATES/shared/artemis-roles.properties $BROKER_CONF_DIR/artemis-roles.properties
    cp $CONFIG_TEMPLATES/shared/artemis-users.properties $BROKER_CONF_DIR/artemis-users.properties
    cp $CONFIG_TEMPLATES/shared/cert-roles.properties $BROKER_CONF_DIR/cert-roles.properties
    cp $CONFIG_TEMPLATES/shared/cert-users.properties /tmp/cert-users.properties
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
    echo "export KEYSTORE_PATH=/etc/enmasse-certs/keystore.p12" >> $BROKER_CUSTOM/bin/env.sh
    echo "export TRUSTSTORE_PATH=/etc/enmasse-certs/truststore.p12" >> $BROKER_CUSTOM/bin/env.sh
    echo "export AUTH_TRUSTSTORE_PATH=$instanceDir/etc/enmasse-authtruststore.jks" >> $BROKER_CUSTOM/bin/env.sh
    echo "export EXTERNAL_KEYSTORE_PATH=$instanceDir/etc/external-keystore.jks" >> $BROKER_CUSTOM/bin/env.sh

    if [ "${GLOBAL_MAX_SIZE}" == "-1" ]; then
        CONTAINER_MEMORY=$(cat /sys/fs/cgroup/memory/memory.limit_in_bytes)
        export GLOBAL_MAX_SIZE=$((${CONTAINER_MEMORY} / 8))
    fi

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

    configure_shared
    envsubst < /tmp/cert-users.properties > $BROKER_CONF_DIR/cert-users.properties

    envsubst < /tmp/login.config > $BROKER_CONF_DIR/login.config
    envsubst < /tmp/broker.xml > $BROKER_CONF_DIR/broker.xml

    cp $CONFIG_TEMPLATES/jolokia-access.xml $BROKER_CONF_DIR/jolokia-access.xml
    cp /opt/broker-plugin/lib/broker-cli.jar $BROKER_CUSTOM/bin/broker-cli.jar

    export ARTEMIS_INSTANCE=${instanceDir}
    export ARTEMIS_INSTANCE_URI=file:${instanceDir}/
    export ARTEMIS_DATA_DIR=${instanceDir}/data
    export ARTEMIS_INSTANCE_ETC_URI=file:${instanceDir}/etc/
    
    envsubst < $CONFIG_TEMPLATES/artemis.profile > $BROKER_CONF_DIR/artemis.profile
    envsubst < $CONFIG_TEMPLATES/management.xml > $BROKER_CONF_DIR/management.xml

    cp $CONFIG_TEMPLATES/logging.properties $BROKER_CONF_DIR/logging.properties
}

function configure_ssl() {
    TRUSTSTORE_PASS=enmasse
    KEYSTORE_PASS=enmasse

    # Use pre-generated store
    mkdir -p $BROKER_CUSTOM/certs
    export CUSTOM_KEYSTORE_PATH=/etc/enmasse-certs/keystore.p12
    export CUSTOM_TRUSTSTORE_PATH=/etc/enmasse-certs/truststore.p12
}

pre_configuration
configure_ssl
echo "broker-plugin is complete"
