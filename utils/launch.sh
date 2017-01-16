#!/bin/sh

. $ARTEMIS_HOME/bin/partitionPV.sh
. $ARTEMIS_HOME/bin/dynamic_resources.sh

export BROKER_IP=`hostname -I | cut -f 1 -d ' '`
CONFIG_TEMPLATES=/config_templates
JAVA_OPTS="-Djava.net.preferIPv4Stack=true"

MAX_HEAP=`get_heap_size`
if [ -n "$MAX_HEAP" ]; then
  JAVA_OPTS="-Xms${MAX_HEAP}m -Xmx${MAX_HEAP}m $JAVA_OPTS"
fi

# Make sure that we use /dev/urandom
JAVA_OPTS="${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom"

# Parameters are
# - instance directory
# - instance id
function configure() {
    local instanceDir=$1
    local instanceId=$2
    export CONTAINER_ID=$HOSTNAME
    if [ ! -d "$INSTANCE" ]; then
        $ARTEMIS_HOME/bin/artemis create $instanceDir --user admin --password admin --role admin --allow-anonymous --java-options "$JAVA_OPTS"
        cp $CONFIG_TEMPLATES/broker_header.xml /tmp/broker.xml
        if [ -n "$QUEUE_NAMES" ]; then
            cat $CONFIG_TEMPLATES/broker_queue_address_header.xml >> /tmp/broker.xml
            for queue in $QUEUE_NAMES
            do
                cat $CONFIG_TEMPLATES/broker_queue_address.xml | QUEUE_NAME=$queue envsubst >> /tmp/broker.xml
            done
            cat $CONFIG_TEMPLATES/broker_queue_address_footer.xml >> /tmp/broker.xml
            cat $CONFIG_TEMPLATES/broker_queue_connector_header.xml >> /tmp/broker.xml
            for queue in $QUEUE_NAMES
            do
                cat $CONFIG_TEMPLATES/broker_queue_connector.xml | QUEUE_NAME=$queue envsubst >> /tmp/broker.xml
            done
            cat $CONFIG_TEMPLATES/broker_queue_connector_footer.xml >> /tmp/broker.xml
        elif [ -n "$TOPIC_NAME" ]; then
            cat $CONFIG_TEMPLATES/broker_topic.xml >> /tmp/broker.xml
        fi
        cat $CONFIG_TEMPLATES/broker_footer.xml >> /tmp/broker.xml
    
        envsubst < /tmp/broker.xml > $instanceDir/etc/broker.xml
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

DATA_DIR="/var/run/artemis/"
if [ -n "$QUEUE_NAMES" ]; then
    ADDRESS=`echo $QUEUE_NAMES | sed -e 's/ /-/g'`
elif [ -n "$TOPIC_NAME" ]; then
    ADDRESS=$TOPIC_NAME
fi
partitionPV "${DATA_DIR}" "${ARTEMIS_LOCK_TIMEOUT:-30}"
