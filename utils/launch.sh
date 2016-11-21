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
    export CONTAINER_ID=$instanceId
    if [ ! -d "$INSTANCE" ]; then
        $ARTEMIS_HOME/bin/artemis create $instanceDir --user admin --password admin --role admin --allow-anonymous --java-options "$JAVA_OPTS"
        cp $CONFIG_TEMPLATES/broker_header.xml /tmp/broker.xml
        if [ -n "$QUEUE_NAME" ]; then
            cat $CONFIG_TEMPLATES/broker_queue.xml >> /tmp/broker.xml
        elif [ -n "$TOPIC_NAME" ]; then
            cat $CONFIG_TEMPLATES/broker_topic.xml >> /tmp/broker.xml
        fi
    
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
if [ -n "$QUEUE_NAME" ]; then
    ADDRESS=$QUEUE_NAME
elif [ -n "$TOPIC_NAME" ]; then
    ADDRESS=$TOPIC_NAME
fi
partitionPV "${DATA_DIR}" "${ADDRESS}" "${ARTEMIS_LOCK_TIMEOUT:-30}"
