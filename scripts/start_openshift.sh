#!/bin/bash
set -x
DIR=$1
mkdir -p logs
sudo $DIR/openshift start 2> logs/os.err > logs/os.log &
sleep 20
sudo $DIR/openshift admin router
