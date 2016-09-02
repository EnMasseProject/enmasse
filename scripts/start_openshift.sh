#!/bin/sh
DIR=$1
mkdir -p logs
$DIR/openshift start 2> logs/os.err > logs/os.log &
