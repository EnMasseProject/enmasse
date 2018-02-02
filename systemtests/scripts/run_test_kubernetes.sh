#!/bin/bash
set -x
CURDIR=`readlink -f \`dirname $0\``
source ${CURDIR}/test_func.sh

ENMASSE_DIR=$1
TESTCASE=$2
failure=0

export OPENSHIFT_URL=${OPENSHIFT_URL:-https://localhost:8443}
export OPENSHIFT_USER=${OPENSHIFT_USER:-test}
export OPENSHIFT_PASSWD=${OPENSHIFT_PASSWD:-test}
export OPENSHIFT_PROJECT=${OPENSHIFT_PROJECT:-enmasseci}
export OPENSHIFT_TEST_LOGDIR=${OPENSHIFT_TEST_LOGDIR:-/tmp/testlogs}
export OPENSHIFT_USE_TLS=${OPENSHIFT_USE_TLS:-true}
export ARTIFACTS_DIR=${ARTIFACTS_DIR:-artifacts}
export DEFAULT_AUTHSERVICE=standard
export USE_MINIKUBE=true

SANITIZED_PROJECT=$OPENSHIFT_PROJECT
SANITIZED_PROJECT=${SANITIZED_PROJECT//_/-}
SANITIZED_PROJECT=${SANITIZED_PROJECT//\//-}
export OPENSHIFT_PROJECT=$SANITIZED_PROJECT

kubectl create namespace $OPENSHIFT_PROJECT
kubectl config set-context $(kubectl config current-context) --namespace=$OPENSHIFT_PROJECT

${ENMASSE_DIR}/deploy-kubernetes.sh -n $OPENSHIFT_PROJECT -o multitenant -a "none standard"

kubectl apply -f ${ENMASSE_DIR}/kubernetes/addons/external-lb.yaml -n ${OPENSHIFT_PROJECT}

#environment info
LOG_DIR="${ARTIFACTS_DIR}/kubernetes-info/"
mkdir -p ${LOG_DIR}
get_kubernetes_info ${LOG_DIR} services default "-before"
get_kubernetes_info ${LOG_DIR} pods default "-before"

run_test ${TESTCASE} systemtests || failure=$(($failure + 1))

#environment info
get_kubernetes_info ${LOG_DIR} pv ${OPENSHIFT_PROJECT}
get_kubernetes_info ${LOG_DIR} pods ${OPENSHIFT_PROJECT} 
get_kubernetes_info ${LOG_DIR} services default "-after"
get_kubernetes_info ${LOG_DIR} pods default "-after"
get_kubernetes_info ${LOG_DIR} events ${OPENSHIFT_PROJECT}

#store artifacts
$CURDIR/collect_logs.sh ${ARTIFACTS_DIR}

if [ $failure -gt 0 ]
then
    echo "Systemtests failed"
    exit 1
else
    teardown_test $OPENSHIFT_PROJECT
fi
