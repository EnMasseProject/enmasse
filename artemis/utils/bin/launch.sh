#!/bin/sh

source /opt/partition/partitionPV.sh
source /usr/local/dynamic-resources/dynamic_resources.sh

export BROKER_IP=`hostname -I | cut -f 1 -d ' '`
CONFIG_TEMPLATES=/config_templates
JAVA_OPTS="-Djava.net.preferIPv4Stack=true -Dcom.sun.management.jmxremote=true -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=true -Dcom.sun.management.jmxremote.registry.ssl=true -Dcom.sun.management.jmxremote.ssl.need.client.auth=true -Dcom.sun.management.jmxremote.authenticate=false -javaagent:/jmx_exporter/jmx_prometheus_javaagent-0.1.0.jar=8080:/etc/prometheus-config/config.yaml"

if [ -n "$ADMIN_SERVICE_HOST" ]
then
    export QUEUE_SCHEDULER_SERVICE_HOST=$ADMIN_SERVICE_HOST
    export QUEUE_SCHEDULER_SERVICE_PORT=$ADMIN_SERVICE_PORT_QUEUE_SCHEDULER
fi

# Make sure that we use /dev/urandom
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

#Set the memory options
JAVA_OPTS="$(adjust_java_options ${JAVA_OPTS})"
#GC Option conflicts with the one already configured.
JAVA_OPTS=$(echo $JAVA_OPTS | sed -e "s/-XX:+UseParallelGC/ /")


function configure_brokered() {

    DISABLE_AUTHORIZATION=$(echo ${DISABLE_AUTHORIZATION-false} | tr '[:upper:]' '[:lower:]')
    if [ "${DISABLE_AUTHORIZATION}" == "true" ]
    then
        export KEYCLOAK_GROUP_PERMISSIONS=false
    else
        export KEYCLOAK_GROUP_PERMISSIONS=true
    fi
    cp $CONFIG_TEMPLATES/brokered/broker.xml /tmp/broker.xml
    cp $CONFIG_TEMPLATES/brokered/login.config /tmp/login.config
    export HAWTIO_ROLE=admin
}

function configure_standard() {
    cp $CONFIG_TEMPLATES/standard/broker_header.xml /tmp/broker.xml
    if [ -n "$TOPIC_NAME" ]; then
        cat $CONFIG_TEMPLATES/standard/broker_topic.xml >> /tmp/broker.xml
    elif [ -n $QUEUE_NAME ] && [ "$QUEUE_NAME" != "" ]; then
        cat $CONFIG_TEMPLATES/standard/broker_queue.xml >> /tmp/broker.xml
    else
        cat $CONFIG_TEMPLATES/standard/broker_queue_colocated.xml >> /tmp/broker.xml
    fi
    cat $CONFIG_TEMPLATES/standard/broker_footer.xml >> /tmp/broker.xml
    cp $CONFIG_TEMPLATES/standard/login.config /tmp/login.config
    export HAWTIO_ROLE=manage
}

# Parameters are
# - instance directory
# - instance id
function configure() {
    local instanceDir=$1
    local instanceId=$2
    export CONTAINER_ID=$HOSTNAME
    if [ ! -d "$INSTANCE" ]; then

        export KEYSTORE_PATH=$instanceDir/etc/enmasse-keystore.jks
        export TRUSTSTORE_PATH=$instanceDir/etc/enmasse-truststore.jks
        export AUTH_TRUSTSTORE_PATH=$instanceDir/etc/enmasse-authtruststore.jks
        export EXTERNAL_KEYSTORE_PATH=$instanceDir/etc/external-keystore.jks
        TRUSTSTORE_PASS=enmasse
        KEYSTORE_PASS=enmasse


        export JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.keyStore=${KEYSTORE_PATH} -Djavax.net.ssl.keyStorePassword=${KEYSTORE_PASS} -Djavax.net.ssl.trustStore=${TRUSTSTORE_PATH} -Djavax.net.ssl.trustStorePassword=${TRUSTSTORE_PASS}"

        $ARTEMIS_HOME/bin/artemis create $instanceDir --user admin --password admin --role admin --allow-anonymous --java-options "$JAVA_OPTS"

        if [ "$ADDRESS_SPACE_TYPE" == "brokered" ]; then
            configure_brokered
        else
            configure_standard
        fi
    
        envsubst < /tmp/broker.xml > $instanceDir/etc/broker.xml
        envsubst < /tmp/login.config > $instanceDir/etc/login.config

        cp $CONFIG_TEMPLATES/bootstrap.xml $instanceDir/etc/bootstrap.xml
        cp $CONFIG_TEMPLATES/jolokia-access.xml $instanceDir/etc/jolokia-access.xml

        # Convert certs
        openssl pkcs12 -export -passout pass:${KEYSTORE_PASS} -in /etc/enmasse-certs/tls.crt -inkey /etc/enmasse-certs/tls.key -chain -CAfile /etc/enmasse-certs/ca.crt -name "io.enmasse" -out /tmp/enmasse-keystore.p12

        keytool -importkeystore -srcstorepass ${KEYSTORE_PASS} -deststorepass ${KEYSTORE_PASS} -destkeystore $KEYSTORE_PATH -srckeystore /tmp/enmasse-keystore.p12 -srcstoretype PKCS12
        keytool -import -noprompt -file /etc/enmasse-certs/ca.crt -alias firstCA -deststorepass ${TRUSTSTORE_PASS} -keystore $TRUSTSTORE_PATH

        keytool -import -noprompt -file /etc/authservice-ca/tls.crt -alias firstCA -deststorepass ${TRUSTSTORE_PASS} -keystore $AUTH_TRUSTSTORE_PATH

        if [ -d /etc/external-certs ]
        then
            openssl pkcs12 -export -passout pass:${KEYSTORE_PASS} -in /etc/external-certs/tls.crt -inkey /etc/external-certs/tls.key -name "io.enmasse" -out /tmp/external-keystore.p12
            keytool -importkeystore -srcstorepass ${KEYSTORE_PASS} -deststorepass ${KEYSTORE_PASS} -destkeystore $EXTERNAL_KEYSTORE_PATH -srckeystore /tmp/external-keystore.p12 -srcstoretype PKCS12

        fi

        export ARTEMIS_INSTANCE=${instanceDir}
        export ARTEMIS_INSTANCE_URI=file:${instanceDir}/
        envsubst < $CONFIG_TEMPLATES/artemis.profile > $instanceDir/etc/artemis.profile

        # cp $CONFIG_TEMPLATES/logging.properties $instanceDir/etc/logging.properties
    fi

}

function init_data_dir() {
# No init needed for Artemis
  return
}

# Parameters are
# - instance directory
# - instance id
function runServer() {
  local instanceDir=$1
  local instanceId=$2
  echo "Configuring instance $instanceId in directory $instanceDir"
  configure $instanceDir $instanceId
  echo "Running instance $instanceId"
  exec $instanceDir/bin/artemis run
}

# This needs to be at the toplevel outside any functions
# For the standard address space, the shutdown hooks need time to run before broker is shut down
if [ "$ADDRESS_SPACE_TYPE" != "brokered" ]; then
    trap "" TERM INT
fi

DATA_DIR="/var/run/artemis/"
partitionPV "${DATA_DIR}" "${ARTEMIS_LOCK_TIMEOUT:-30}"
