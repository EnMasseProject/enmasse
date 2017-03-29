#!/bin/bash
DIR=$1
mkdir -p logs
sudo $DIR/oc cluster up
