#!/bin/bash
ENMASSE_DIR=$1
SYSTEMTESTS=$2
DIR=`readlink -f \`dirname $0\``
source $DIR/common.sh
failure=0
OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
OPENSHIFT_USER=${OPENSHIFT_USER:-test}
OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
MULTITENANT=${MULTITENANT:-false}

oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${OPENSHIFT_URL}

setup_test $OPENSHIFT_PROJECT $ENMASSE_DIR $MULTITENANT $OPENSHIFT_URL $OPENSHIFT_USER

pushd $SYSTEMTESTS
run_test $OPENSHIFT_PROJECT true $MULTITENANT $OPENSHIFT_URL $OPENSHIFT_USER || failure=$(($failure + 1))
popd

$SYSTEMTESTS/scripts/collect_logs.sh $ARTIFACTS_DIR

oc get pods
oc get deployments

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
else
    teardown_test $OPENSHIFT_PROJECT
fi
