#!/bin/bash
DIR=$1
mkdir -p logs
sudo $DIR/openshift start 2> logs/os.err > logs/os.log &
