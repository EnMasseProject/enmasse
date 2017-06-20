#!/bin/bash
ENMASSE_DIR=$1
DIR=`dirname $0`
source $DIR/common.sh
failure=0
OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
OPENSHIFT_USER=${OPENSHIFT_USER:-test}
OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
MULTITENANT=${MULTITENANT:-false}

oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${OPENSHIFT_URL}

setup_test $OPENSHIFT_PROJECT $ENMASSE_DIR $MULTITENANT $OPENSHIFT_URL $OPENSHIFT_USER
run_test $OPENSHIFT_PROJECT true $MULTITENANT $OPENSHIFT_URL $OPENSHIFT_USER || failure=$(($failure + 1))
teardown_test $OPENSHIFT_PROJECT

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
fi
