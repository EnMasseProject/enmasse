#!/usr/bin/env bash
LOG_DIR=$1

mkdir -p $LOG_DIR

while [ true ]; do
    sudo rsync -avL /var/log/containers/* $LOG_DIR/
    sudo chmod -R 777 $LOG_DIR
    sleep 5
done
