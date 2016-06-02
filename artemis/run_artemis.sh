#!/bin/sh

VOLUME="/var/run/artemis/"
BASE=$(dirname $0)
INSTANCE=$($BASE/get_free_instance.py $VOLUME)
if [ ! -d "$INSTANCE" ]; then
    $ARTEMIS_HOME/bin/artemis create $INSTANCE --user admin --password admin --role admin --allow-anonymous
    if [ -n "$QUEUE_NAME" ]; then
        cat > /tmp/snippet <<EOF
      <queues>
          <queue name="$QUEUE_NAME">
             <address>$QUEUE_NAME</address>
          </queue>
      </queues>
EOF
        sed -i -e '/<\/address-settings>/r /tmp/snippet' $INSTANCE/etc/broker.xml
        sed -i -e 's/0.0.0.0:5672/0.0.0.0:5673/' $INSTANCE/etc/broker.xml
    elif [ -n "$TOPIC_NAME" ]; then
        echo "ERROR: topics not yet supported"
    fi
fi

exec $INSTANCE/bin/artemis run
