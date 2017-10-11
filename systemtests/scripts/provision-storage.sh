#!/usr/bin/env bash
MY_DIR=$1
PV_NAME=$2

mkdir -p ${MY_DIR}
chmod -R 777 ${MY_DIR}
oc process -f templates/include/persistent-volume.json HOST_DIR=${MY_DIR} NAME=${PV_NAME} | oc create -f -
