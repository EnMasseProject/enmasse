#!/bin/sh
export BROKER_IP=`hostname -I | cut -f 1 -d ' '`
CONFIG_TEMPLATES=/config_templates
VOLUME="/var/run/artemis/"
BASE=$(dirname $0)
INSTANCE=$($BASE/get_free_instance.py $VOLUME)
CONTAINER_ID=$(basename $INSTANCE)
export CONTAINER_ID
if [ ! -d "$INSTANCE" ]; then
    $ARTEMIS_HOME/bin/artemis create $INSTANCE --user admin --password admin --role admin --allow-anonymous --java-options "-Djava.net.preferIPv4Stack=true"
    cp $CONFIG_TEMPLATES/broker_header.xml /tmp/broker.xml
    if [ -n "$QUEUE_NAME" ]; then
        cat $CONFIG_TEMPLATES/broker_queue.xml >> /tmp/broker.xml
    fi
    cat $CONFIG_TEMPLATES/broker_footer.xml >> /tmp/broker.xml

    envsubst < /tmp/broker.xml > $INSTANCE/etc/broker.xml
fi

exec $INSTANCE/bin/artemis run
