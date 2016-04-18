#!/bin/sh

VOLUME="/var/run/artemis/"
BASE=$(dirname $0)
INSTANCE=$($BASE/get_free_instance.py $VOLUME)
if [ ! -d "$INSTANCE" ]; then
    $ARTEMIS_HOME/bin/artemis create $INSTANCE --user admin --password admin --role admin --allow-anonymous
fi

exec $INSTANCE/bin/artemis run
