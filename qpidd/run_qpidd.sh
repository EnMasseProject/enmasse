#!/bin/sh

VOLUME="/var/run/qpidd/"
BASE=$(dirname $0)
DATA_DIR=$($BASE/get_data_dir.py $VOLUME)
exec /sbin/qpidd --auth no --queue-pattern '/queue/' --topic-pattern '/topic/' --data-dir "$VOLUME$DATA_DIR" --federation_tag $DATA_DIR
