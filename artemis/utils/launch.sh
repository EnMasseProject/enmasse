#!/bin/sh
. $ARTEMIS_HOME/bin/partitionPV.sh
. $ARTEMIS_HOME/bin/dynamic_resources.sh

export BROKER_IP=`hostname -I | cut -f 1 -d ' '`
CONFIG_TEMPLATES=/config_templates
JAVA_OPTS="-Djava.net.preferIPv4Stack=true -javaagent:/jmx_exporter/jmx_prometheus_javaagent-0.1.0.jar=8080:/jmx_exporter/config.yaml"

if [ -n "$ADMIN_SERVICE_HOST" ]
then
    export QUEUE_SCHEDULER_SERVICE_HOST=$ADMIN_SERVICE_HOST
    export QUEUE_SCHEDULER_SERVICE_PORT=$ADMIN_SERVICE_PORT_QUEUE_SCHEDULER
fi

MAX_HEAP=`get_heap_size`
if [ -n "$MAX_HEAP" ]; then
  JAVA_OPTS="-Xms${MAX_HEAP}m -Xmx${MAX_HEAP}m $JAVA_OPTS"
fi

# Make sure that we use /dev/urandom
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

function configure_brokered() {
    cp $CONFIG_TEMPLATES/brokered/broker.xml /tmp/broker.xml
    cp $CONFIG_TEMPLATES/brokered/login.config /tmp/login.config
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
}

# Parameters are
# - instance directory
# - instance id
function configure() {
    local instanceDir=$1
    local instanceId=$2
    export CONTAINER_ID=$HOSTNAME
    if [ ! -d "$INSTANCE" ]; then
        $ARTEMIS_HOME/bin/artemis create $instanceDir --user admin --password admin --role admin --allow-anonymous --java-options "$JAVA_OPTS"

        if [ "$ADDRESS_SPACE_TYPE" == "brokered" ]; then
            configure_brokered
        else
            configure_standard
        fi

        export KEYSTORE_PATH=$instanceDir/etc/enmasse-keystore.jks
        export TRUSTSTORE_PATH=$instanceDir/etc/enmasse-truststore.jks
        export AUTH_TRUSTSTORE_PATH=$instanceDir/etc/enmasse-authtruststore.jks
        export EXTERNAL_KEYSTORE_PATH=$instanceDir/etc/external-keystore.jks
    
        envsubst < /tmp/broker.xml > $instanceDir/etc/broker.xml
        if [ -f /tmp/login.config ]; then
                envsubst < /tmp/login.config > $instanceDir/etc/login.config
        fi
        cp $CONFIG_TEMPLATES/bootstrap.xml $instanceDir/etc/bootstrap.xml

        # Convert certs
        openssl pkcs12 -export -passout pass:enmasse -in /etc/enmasse-certs/tls.crt -inkey /etc/enmasse-certs/tls.key -chain -CAfile /etc/enmasse-certs/ca.crt -name "io.enmasse" -out /tmp/enmasse-keystore.p12

        keytool -importkeystore -srcstorepass enmasse -deststorepass enmasse -destkeystore $KEYSTORE_PATH -srckeystore /tmp/enmasse-keystore.p12 -srcstoretype PKCS12
        keytool -import -noprompt -file /etc/enmasse-certs/ca.crt -alias firstCA -deststorepass enmasse -keystore $TRUSTSTORE_PATH

        keytool -import -noprompt -file /etc/authservice-ca/tls.crt -alias firstCA -deststorepass enmasse -keystore $AUTH_TRUSTSTORE_PATH

        if [ -d /etc/external-certs ]
        then
            openssl pkcs12 -export -passout pass:enmasse -in /etc/external-certs/tls.crt -inkey /etc/external-certs/tls.key -name "io.enmasse" -out /tmp/external-keystore.p12
            keytool -importkeystore -srcstorepass enmasse -deststorepass enmasse -destkeystore $EXTERNAL_KEYSTORE_PATH -srckeystore /tmp/external-keystore.p12 -srcstoretype PKCS12

        fi


        #cp $CONFIG_TEMPLATES/logging.properties $instanceDir/etc/logging.properties

    fi

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
