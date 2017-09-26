#!/bin/bash
ENMASSE_DIR=$1
OC_PATH=$2
SYSTEMTESTS=$3
DIR=`readlink -f \`dirname $0\``
source $DIR/common.sh
failure=0
OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
OPENSHIFT_USER=${OPENSHIFT_USER:-test}
OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
MULTITENANT=${MULTITENANT:-true}

export PATH="$OC_PATH:$PATH"
CONFIG=$OC_PATH/config
export KUBEADM=$CONFIG/master/admin.kubeconfig

oc login -u ${OPENSHIFT_USER} -p ${OPENSHIFT_PASSWD} --insecure-skip-tls-verify=true ${OPENSHIFT_URL}
oc project ${OPENSHIFT_PROJECT}
setup_test $OPENSHIFT_PROJECT $ENMASSE_DIR $MULTITENANT $OPENSHIFT_URL $OPENSHIFT_USER

pushd $SYSTEMTESTS
run_test $OPENSHIFT_PROJECT true $MULTITENANT $OPENSHIFT_URL $OPENSHIFT_USER || failure=$(($failure + 1))
popd

$SYSTEMTESTS/scripts/collect_logs.sh $OC_PATH $ARTIFACTS_DIR

oc get pods

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
else
    teardown_test $OPENSHIFT_PROJECT
fi
